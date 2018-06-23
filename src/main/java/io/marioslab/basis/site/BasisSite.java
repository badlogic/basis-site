
package io.marioslab.basis.site;

import java.io.BufferedOutputStream;
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

import com.sun.nio.file.SensitivityWatchEventModifier;

import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader.FileTemplateLoader;

public class BasisSite {

	public BasisSite (Configuration config) {
		validateConfiguration(config);
		generate(config);
		if (config.isWatch()) {
			try {
				WatchService service = FileSystems.getDefault().newWatchService();
				Path watchDir = config.getInput().toPath();
				WatchService watcher = FileSystems.getDefault().newWatchService();
				watchDir.register(watcher,
					new WatchEvent.Kind[] {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY},
					SensitivityWatchEventModifier.HIGH);

				while (true) {
					log("Watching input directory " + config.getInput().getPath());
					WatchKey key = watcher.take();

					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();
						if (kind == StandardWatchEventKinds.OVERFLOW) continue;
					}

					if (!key.reset()) {
						error("Watching input directory for changes failed.");
					}

					log("File changes detected.");
					generate(config);
				}

			} catch (IOException | InterruptedException e) {
				error("Watching input directory for changes failed. " + e.getMessage());
			}
		}
	}

	private void generate (Configuration config) {
		if (config.isDeleteOutput()) deleteAndCreateOutput(config);
		log("Generating site.");

		generate(config.getInput(), config);
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
				if (input.getName().contains(".bt.")) {
					Template template = new FileTemplateLoader(input.getParentFile()).load(input.getName());
					try (OutputStream out = new FileOutputStream(output)) {
						TemplateContext context = new TemplateContext();
						context.set("file", input);

						try {
							template.render(context, out);
						} catch (Throwable e) {
							log("Error: Couldn't render templated file " + input.getPath() + ".");
							log(e.getMessage());
						}
					}
				} else {
					Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				throw new RuntimeException("Couldn't generate " + output.getPath() + " from file " + input.getPath() + ".", e);
			}
		}
	}

	private void deleteAndCreateOutput (Configuration config) {
		log("Deleting output directory " + config.getOutput().getPath() + ".");
		FileUtils.delete(config.getOutput());
		if (!config.getOutput().mkdirs()) {
			error("Couldn't create output directory " + config.getOutput().getPath() + ".");
		}
	}

	private void validateConfiguration (Configuration config) {
		if (!config.getInput().exists()) error("Input directory " + config.getInput().getPath() + " does not exist.");

		if (config.getOutput().exists()) {
			if (!config.getOutput().isDirectory()) {
				error("Output directory " + config.getOutput().getPath() + " is an existing file.");
			} else {
				warning("Output directory " + config.getOutput().getPath() + " exists.");
			}
		} else {
			if (!config.getOutput().mkdirs()) {
				error("Couldn't create output directory " + config.getOutput().getPath() + ".");
			}
		}
	}

	public static void main (String[] args) {
		Configuration config = parseArguments(args);
		new BasisSite(config);
	}

	public static Configuration parseArguments (String[] args) {
		File input = null;
		File output = null;
		boolean deleteOutput = false;
		boolean watch = false;

		int i = 0;
		while (i < args.length) {
			String arg = args[i];

			if (arg.equals("-i")) {
				i++;
				if (args.length == i) error("Expected an input directory");
				input = new File(args[i]);
			} else if (arg.equals("-o")) {
				i++;
				if (args.length == i) error("Expected an output directory");
				output = new File(args[i]);
			} else if (arg.equals("-w")) {
				watch = true;
			} else if (arg.equals("-d")) {
				deleteOutput = true;
			} else {
				error("Unknown argument '" + arg + "'");
			}
			i++;
		}

		if (input == null) error("Expected an input directory.");
		if (output == null) error("Expected an output directory.");

		return new Configuration(input, output, deleteOutput, watch);
	}

	private static void printHelp () {
		System.out.println("Usage: java -jar basis-site.jar -i <input-directory> -o <output-directory>");
		System.out.println();
		System.out.println("-i <input-directory>    The directory to read the source files from.");
		System.out.println("-o <output-directory>   The directory to write the output to.");
		System.out.println("-d                      Delete the output directory.");
		System.out.println("-w                      Watch the input directory for changes and ");
		System.out.println("                        regenerate the site.");
	}

	private static void log (String message) {
		System.out.println(message);
	}

	private static void error (String message) {
		System.err.println("Error: " + message);
		printHelp();
		System.exit(-1);
	}

	private static void warning (String message) {
		System.out.println("Warning: " + message);
	}
}
