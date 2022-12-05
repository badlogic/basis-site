
package io.marioslab.basis.site;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.esotericsoftware.minlog.Log;

import io.marioslab.basis.arguments.Argument;
import io.marioslab.basis.arguments.ArgumentWithValue.StringArgument;
import io.marioslab.basis.arguments.Arguments;
import io.marioslab.basis.arguments.Arguments.ParsedArguments;
import io.marioslab.basis.site.SiteGenerator.SiteGeneratorException;
import io.marioslab.basis.site.processors.TemplateFileProcessor;
import io.marioslab.basis.site.processors.TemplateFileProcessor.BuiltinFunctionProvider;
import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.helpers.NOPLogger;

/** Command line application for generating static websites. See <a href="https://github.com/badlogic/basis-site">the
 * documentation</a>. **/
public class BasisSite {
	private final SiteGenerator generator;
	private final boolean watch;
	private final boolean deleteOutputDirectory;

	/** Constructs a basis site from {@link ParsedArguments} as created by {@link #createDefaultArguments()}. Throws a
	 * {@link SiteGeneratorException} if the arguments are invalid. **/
	public BasisSite (ParsedArguments args) {
		watch = args.has("-w");
		deleteOutputDirectory = args.has("-d");
		File inputDirectory = new File((String)args.getValue("-i"));
		File outputDirectory = new File((String)args.getValue("-o"));
		if (args.has("-v")) Log.set(Log.LEVEL_DEBUG);

		if (!inputDirectory.exists()) {
			throw new SiteGeneratorException("Input directory " + inputDirectory.getPath() + " does not exist.");
		}

		if (!outputDirectory.exists()) {
			if (!outputDirectory.mkdirs()) {
				throw new SiteGeneratorException("Couldn't create output directory " + outputDirectory.getPath() + ".");
			}
		}

		generator = new SiteGenerator(inputDirectory, outputDirectory);
		generator.addProcessor(new TemplateFileProcessor(Arrays.asList(new BuiltinFunctionProvider(generator))));
	}

	/** Constructs a new basis site.
	 * @param generator The {@link SiteGenerator} to use to generate the site.
	 * @param watch Whether to watch the input directory for changes.
	 * @param deleteOutputDirectory Whether to delete the output directory before (re-)generating from the input. */
	public BasisSite (SiteGenerator generator, boolean watch, boolean deleteOutputDirectory) {
		this.generator = generator;
		this.watch = watch;
		this.deleteOutputDirectory = deleteOutputDirectory;
	}

	private static void deleteFile (File file, boolean first) {
		if (!file.exists()) return;

		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children == null) throw new RuntimeException("Could not read files in directory " + file.getPath());
			for (File child : children) {
				deleteFile(child, false);
			}
			if (!file.delete()) throw new RuntimeException("Could not delete directory " + file.getPath());
		} else {
			if (!file.delete()) throw new RuntimeException("Could not delete file " + file.getPath());
		}
	}

	private void deleteAndCreateOutput () {
		File output = generator.getOutputDirectory();
		Log.info("Deleting output directory " + output.getPath() + ".");
		deleteFile(output, true);
		if (!output.mkdirs()) {
			Log.error("Couldn't create output directory " + output.getPath() + ".");
			System.exit(-1);
		}
	}

	/** Adds a {@link SiteFileProcessor} to this generator. The processors will be applied to input files in the order they have
	 * been added. **/
	public synchronized void addProcessor (SiteFileProcessor processor) {
		generator.addProcessor(processor);
	}

	/** Replaces the SiteFileProcessor of the same class with the provided processor, or appends it at the end of the procesor
	 * list. */
	public synchronized void replaceProcessor (SiteFileProcessor processor) {
		generator.replaceProcessor(processor);
	}

	/** Returns the {@link SiteGenerator}. Use the {@link #addProcessor(SiteFileProcessor)} and
	 * {@link #replaceProcessor(SiteFileProcessor)} methods to modify it for thread-safety. This method only exists as some
	 * {@link SiteFileProcessor} instances may require a generator to be constructed. */
	public SiteGenerator getGenerator () {
		return generator;
	}

	/** Generates the output from the input and optionally enters a loop that watches for input folder changes and re-generates the
	 * site. **/
	public synchronized void generate () {
		generate( () -> {
		});
	}

	/** Generates the output from the input and optionally enters a loop that watches for input folder changes and re-generates the
	 * site. Calls the Runnable after each successful re-generation. **/
	public synchronized void generate (Runnable callback) {
		if (deleteOutputDirectory) deleteAndCreateOutput();

		if (watch) {
			long start = System.nanoTime();
			try {
				generator.generate( (file) -> {
					Log.info("Processed " + file.getInput().getPath() + " -> " + file.getOutput().getPath());
				});
				callback.run();
			} catch (Throwable t) {
				Log.error(t.getMessage());
				Log.debug("Exception", t);
			}

			Log.info("Generating output took: " + String.format("%.2f", (System.nanoTime() - start) / 1000000000f) + " secs");
			Log.info("Watching input directory " + generator.getInputDirectory().getPath());
			try {
				DirectoryWatcher watcher = DirectoryWatcher.builder()
						.path(generator.getInputDirectory().toPath())
						.listener((event) -> {
							long startInner = System.nanoTime();
							try {
								if (deleteOutputDirectory) deleteAndCreateOutput();
								generator.generate( (file) -> {
									Log.info("Processed " + file.getInput().getPath() + " -> " + file.getOutput().getPath());
								});
								callback.run();
							} catch (Throwable t) {
								Log.error(t.getMessage());
								Log.debug("Exception", t);
							}
							Log.info("Generating output took: " + String.format("%.2f", (System.nanoTime() - startInner) / 1000000000f) + " secs");
							Log.info("Watching input directory " + generator.getInputDirectory().getPath());
						}).build();
				watcher.watch();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			generator.generate( (file) -> {
				long start = System.nanoTime();
				Log.info("Processed " + file.getInput().getPath() + " -> " + file.getOutput().getPath());
				Log.info("Generating output took: " + String.format("%.2f", (System.nanoTime() - start) / 1000000000f) + " secs");
			});
			callback.run();
		}
	}

	public static Arguments createDefaultArguments () {
		Arguments args = new Arguments();
		args.addArgument(new StringArgument("-i", "The directory to read the source files from.", "<input-directory>", false));
		args.addArgument(new StringArgument("-o", "The directory to write the output files to.", "<input-directory>", false));
		args.addArgument(new Argument("-d", "Delete the output directory.", true));
		args.addArgument(new Argument("-w", "Watch the input directory for changes and\nregenerate the site.", true));
		args.addArgument(new Argument("-v", "Verbosely log everything.", true));
		args.addArgument(new Argument("-h", "Prints this help text.", true));
		return args;
	}

	public static void main (String[] cliArgs) {
		Arguments args = createDefaultArguments();
		try {
			ParsedArguments parsedArgs = args.parse(cliArgs);
			if (parsedArgs.has("-h")) {
				System.out.println("Usage: java -jar basis-site.jar <options>");
				System.out.println(args.printHelp());
				System.exit(0);
			}
			BasisSite site = new BasisSite(parsedArgs);
			site.generate();
		} catch (SiteGeneratorException e) {
			Log.error(e.getMessage(), e);
			args.printHelp(System.out);
			System.exit(-1);
		}
	}
}
