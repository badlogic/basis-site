
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

	private static void deleteDirectory (File file, boolean first) {
		if (!file.exists()) return;

		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children == null) throw new RuntimeException("Could not read files in directory " + file.getPath());
			for (File child : children) {
				delete(child, false);
			}
			if (!file.delete()) throw new RuntimeException("Could not delete directory " + file.getPath());
		} else {
			if (!file.delete()) throw new RuntimeException("Could not delete file " + file.getPath());
		}
	}

	private void generate (Configuration config) {
		try {
			if (config.isDeleteOutput()) deleteAndCreateOutput(config);
			log("Generating site.");

		} catch (RuntimeException e) {
			error(e.getMessage());
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

	public static void warning (String message) {
		System.out.println("Warning: " + message);
	}
}
