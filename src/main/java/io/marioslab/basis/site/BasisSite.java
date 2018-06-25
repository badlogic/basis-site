
package io.marioslab.basis.site;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.sun.nio.file.SensitivityWatchEventModifier;

import io.marioslab.basis.site.Configuration.ConfigurationExtension;
import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader.FileTemplateLoader;

@SuppressWarnings("restriction")
public class BasisSite {

	@SuppressWarnings("unchecked")
	public BasisSite (Configuration config) {
		validateConfiguration(config);
		generate(config);
		if (config.isWatch()) {
			try {
				WatchService watcher = FileSystems.getDefault().newWatchService();
				registerDirectories(watcher, config.getInput());

				while (true) {
					log("Watching input directory " + config.getInput().getPath());
					WatchKey key = watcher.take();

					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();
						if (kind == StandardWatchEventKinds.OVERFLOW) continue;

						WatchEvent<Path> ev = (WatchEvent<Path>)event;
						Path filename = ev.context();
						File file = filename.toFile();
						if (file.exists() && file.isDirectory()) {
							registerDirectories(watcher, file);
						}
					}

					if (!key.reset()) {
						fatalError("Watching input directory for changes failed.", false);
					}

					log("File changes detected.");

					generate(config);
				}

			} catch (IOException | InterruptedException e) {
				fatalError("Watching input directory for changes failed. " + e.getMessage(), false);
			}
		}
	}

	private void registerDirectories (WatchService watcher, File dir) throws IOException {
		if (!dir.isDirectory()) return;
		dir.toPath().register(watcher,
			new WatchEvent.Kind[] {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY},
			SensitivityWatchEventModifier.HIGH);

		File[] children = dir.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory()) registerDirectories(watcher, child);
			}
		}

	}

	private void generate (Configuration config) {
		try {
			if (config.isDeleteOutput()) deleteAndCreateOutput(config);
			log("Generating site.");

			generate(config.getInput(), config);
		} catch (RuntimeException e) {
			error(e.getMessage());
		}
	}

	private void generate (File input, Configuration config) {
		if (input.getName().startsWith("_")) return;

		if (!input.exists()) return;

		File output = new File(config.getOutput(), input.getAbsolutePath().replace(".bt.", ".").replace(config.getInput().getAbsolutePath(), ""));

		if (input.isDirectory()) {
			if (!output.exists() && !output.mkdirs()) throw new RuntimeException("Couldn't create output directory " + output.getPath() + ".");
			File[] children = input.listFiles();
			if (children == null) throw new RuntimeException("Couldn't read directory " + input.getPath() + ".");
			for (File child : children) {
				generate(child, config);
			}
		} else {
			try {
				Map<String, Object> metadata = FileUtils.readMetadataBlock(input);

				if (input.getName().contains(".bt.")) {
					Template template = null;
					if (metadata == null) {
						template = new FileTemplateLoader().load(input.getPath());
					} else {
						template = new FileTemplateLoader() {
							@Override
							protected Source loadSource (String path) {
								if (path.equals(input.getPath())) {
									return new Source(path, FileUtils.stripMetadataBlock(input));
								} else {
									return super.loadSource(path);
								}
							}
						}.load(input.getPath());
					}
					try (OutputStream out = new FileOutputStream(output)) {
						TemplateContext context = new TemplateContext();
						String inputPath = input.getParent().indexOf('/') >= 0 ? input.getParent().substring(input.getParent().indexOf('/') + 1) : input.getParent();
						String outputPath = output.getParent().indexOf('/') >= 0 ? output.getParent().substring(output.getParent().indexOf('/') + 1)
							: input.getParent();
						context.set("file", new SiteFile(inputPath, input.getName(), outputPath, output.getName(), false, metadata));

						for (FunctionProvider provider : config.getFunctionProviders()) {
							provider.provide(input, output, config, context);
						}

						try {
							template.render(context, out);
						} catch (Throwable e) {
							error("Couldn't render templated file " + input.getPath() + ".");
							System.err.println(e.getMessage());
						}
					}
				} else {
					if (metadata != null) {
						FileUtils.writeFile(FileUtils.stripMetadataBlock(input), output);
					} else {
						Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				}
			} catch (Throwable e) {
				if (e.getMessage() != null) {
					error("Couldn't generate output from file " + input.getPath() + ".");
					System.err.println(e.getMessage());
				} else {
					error("Couldn't generate " + output.getPath() + " from file " + input.getPath() + ".");
					e.printStackTrace();
				}
			}
		}
	}

	private void deleteAndCreateOutput (Configuration config) {
		log("Deleting output directory " + config.getOutput().getPath() + ".");
		FileUtils.delete(config.getOutput(), true);
		if (!config.getOutput().mkdirs()) {
			fatalError("Couldn't create output directory " + config.getOutput().getPath() + ".", false);
		}
	}

	private void validateConfiguration (Configuration config) {
		if (!config.getInput().exists()) fatalError("Input directory " + config.getInput().getPath() + " does not exist.", false);

		if (config.getOutput().exists()) {
			if (!config.getOutput().isDirectory()) {
				fatalError("Output directory " + config.getOutput().getPath() + " is an existing file.", false);
			} else {
				warning("Output directory " + config.getOutput().getPath() + " exists.");
			}
		} else {
			if (!config.getOutput().mkdirs()) {
				fatalError("Couldn't create output directory " + config.getOutput().getPath() + ".", false);
			}
		}
	}

	public static void main (String[] args) {
		Configuration config = Configuration.parse(args);
		new BasisSite(config);
	}

	private static void printHelp (ConfigurationExtension... extensions) {
		System.out.println("Usage: java -jar basis-site.jar -i <input-directory> -o <output-directory>");
		System.out.println();
		System.out.println("-i <input-directory>    The directory to read the source files from.");
		System.out.println("-o <output-directory>   The directory to write the output to.");
		System.out.println("-d                      Delete the output directory.");
		System.out.println("-w                      Watch the input directory for changes and ");
		System.out.println("                        regenerate the site.");
		for (ConfigurationExtension ext : extensions) {
			ext.printHelp();
		}
	}

	public static void log (String message) {
		System.out.println(message);
	}

	public static void error (String message) {
		System.err.println("Error: " + message);
	}

	public static void fatalError (String message, boolean printHelp, ConfigurationExtension... extensions) {
		System.err.println("Error: " + message);
		if (printHelp) printHelp(extensions);
		System.exit(-1);
	}

	public static void warning (String message) {
		System.out.println("Warning: " + message);
	}
}
