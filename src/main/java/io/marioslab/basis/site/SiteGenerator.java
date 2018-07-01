
package io.marioslab.basis.site;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import io.marioslab.basis.template.Error.TemplateException;

/** Takes an input directory, transforms the files via a list of {@link SiteFileProcessor} instances, and writes the results to an
 * output directory. */
public class SiteGenerator {
	/** Optional callback to be invoked for each file that is successfully processed by this generator. See
	 * {@link SiteGenerator#generate(SiteGeneratorCallback)}. **/
	public interface SiteGeneratorCallback {
		public void generated (SiteFile file);
	}

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

	private final File inputDirectory;
	private final File outputDirectory;
	private final List<SiteFileProcessor> processors = new ArrayList<>();

	/** Constructs a new site generator that processes the files in the input directory via the list of {@link SiteFileProcessor}
	 * instances (as returned by {@link #getProcessors()}) and writes the results to the output directory. **/
	public SiteGenerator (File inputDirectory, File outputDirectory) {
		this.inputDirectory = inputDirectory;
		this.outputDirectory = outputDirectory;
	}

	/** Returns the input directory from which files will be read and processed. **/
	public File getInputDirectory () {
		return inputDirectory;
	}

	/** Returns the output directory to which files will be written. **/
	public File getOutputDirectory () {
		return outputDirectory;
	}

	/** Returns the list of {@link SiteFileProcessor} instances used to process input files. **/
	public List<SiteFileProcessor> getProcessors () {
		return processors;
	}

	/** Transforms the files in the input directory via a list of {@link SiteFileProcessor} instances, and writes the results to an
	 * output directory. Files and directories starting with "_" will be ignored. Throws a {@link SiteGeneratorException} in case
	 * anything went wrong. When an error occurs, files written until that point will not be cleaned up. */
	public void generate () {
		generate(inputDirectory, inputDirectory, outputDirectory, processors, null);
	}

	/** Transforms the files in the input directory via a list of {@link SiteFileProcessor} instances, and writes the results to an
	 * output directory. Files and directories starting with "_" will be ignored. Throws a {@link SiteGeneratorException} in case
	 * anything went wrong. When an error occurs, files written until that point will not be cleaned up. For each successfully
	 * processed file, the {@link SiteGeneratorCallback} will be called. */
	public void generate (SiteGeneratorCallback callback) {
		generate(inputDirectory, inputDirectory, outputDirectory, processors, callback);
	}

	/** Processes a single file or directory via the list of {@link SiteFileProcessor} instances. **/
	private void generate (File inputFile, File inputDirectory, File outputDirectory, List<SiteFileProcessor> processors, SiteGeneratorCallback callback) {
		// Ignore files starting with "_" or non-existing files
		if (inputFile.getName().startsWith("_") || !inputFile.exists()) return;

		// If this is a directory, generate the output directory and recurse
		if (inputFile.isDirectory()) {
			File outputFile = new File(outputDirectory, inputFile.getAbsolutePath().replace(inputDirectory.getAbsolutePath(), ""));
			if (!outputFile.exists() && !outputFile.mkdirs()) throw new SiteGeneratorException("Couldn't create output directory " + outputFile.getPath() + ".");
			File[] children = inputFile.listFiles();
			if (children == null) throw new SiteGeneratorException("Couldn't read directory " + inputFile.getPath() + ".");
			for (File child : children)
				generate(child, inputDirectory, outputDirectory, processors, callback);
			return;
		}

		// Otherwise, load the file content and run it through the processors
		try {
			File outputFile = generateOutputFile(inputFile);

			SiteFile file = new SiteFile(inputFile, outputFile, Files.readAllBytes(inputFile.toPath()));
			for (SiteFileProcessor processor : processors)
				processor.process(file);
			Files.write(outputFile.toPath(), file.getContent());
			if (callback != null) callback.generated(file);
		} catch (Throwable t) {
			if (t instanceof TemplateException) {
				throw (TemplateException)t;
			} else {
				throw new SiteGeneratorException("Couldn't generate output for file " + inputFile.getPath() + ".", t);
			}
		}
	}

	/** Generates the output file by passing the input file through the list of {@link SiteFileProcessor} instances of this
	 * generator, calling each processor's {@link SiteFileProcessor#process(SiteFile) method, and calculating the end result
	 * relative to the output directory. **/
	public File generateOutputFile (File inputFile) {
		File outputFile = new File(outputDirectory, inputFile.getAbsolutePath().replace(inputDirectory.getAbsolutePath(), ""));
		String outputFileName = outputFile.getName();
		for (SiteFileProcessor processor : processors) {
			outputFileName = processor.processOutputFileName(outputFileName);
		}
		return new File(outputFile.getParent() == null ? "" : outputFile.getParent(), outputFileName);
	}
}
