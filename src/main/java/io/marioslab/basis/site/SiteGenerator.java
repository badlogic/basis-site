
package io.marioslab.basis.site;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/** Takes an input directory, transforms the files via a list of {@link SiteFileProcessor} instances, and writes the results to an
 * output directory. */
public class SiteGenerator {

	/** Exception used for all error reporting. **/
	@SuppressWarnings("serial")
	public static class SiteGeneratorException extends RuntimeException {
		public SiteGeneratorException (String message, Throwable cause) {
			super(message, cause);
		}

		public SiteGeneratorException (String message) {
			super(message);
		}
	}

	/** Takes an input directory, transforms the files via a list of {@link SiteFileProcessor} instances, and writes the results to
	 * an output directory. Files and directories starting with "_" will be ignored. Throws a {@link SiteGeneratorException} in
	 * case anything went wrong. When an error occurs, files written until that point will not be cleaned up. */
	public void generate (File inputDirectory, File outputDirectory, List<SiteFileProcessor> processors) {
		generate(inputDirectory, inputDirectory, outputDirectory, processors);
	}

	/** Processes a single file or directory via the file processors **/
	private void generate (File inputFile, File inputDirectory, File outputDirectory, List<SiteFileProcessor> processors) {
		// Ignore files starting with "_" or non-existing files
		if (inputFile.getName().startsWith("_") || !inputFile.exists()) return;

		// Derive the output file path
		File outputFile = new File(outputDirectory, inputFile.getAbsolutePath().replace(inputDirectory.getAbsolutePath(), ""));

		// If this is a directory, generate the output directory and recurse
		if (inputFile.isDirectory()) {
			if (!outputFile.exists() && !outputFile.mkdirs()) throw new SiteGeneratorException("Couldn't create output directory " + outputFile.getPath() + ".");
			File[] children = inputFile.listFiles();
			if (children == null) throw new SiteGeneratorException("Couldn't read directory " + inputFile.getPath() + ".");
			for (File child : children)
				generate(child, inputDirectory, outputDirectory, processors);
			return;
		}

		// Otherwise, load the file content and run it through the processors
		try {
			SiteFile file = new SiteFile(inputFile, outputFile, Files.readAllBytes(inputFile.toPath()));
			for (SiteFileProcessor processor : processors)
				processor.process(file);
			Files.write(outputFile.toPath(), file.content);
		} catch (Throwable t) {
			throw new SiteGeneratorException("Couldn't generate output for file " + inputFile.getPath() + ".", t);
		}
	}
}
