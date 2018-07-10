
package io.marioslab.basis.site;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** A file to be processed by a {@link SiteFileProcessor}. **/
public class SiteFile {
	private final File input;
	private final File output;
	private byte[] content;
	private final Map<String, Object> metadata;

	/** Creates a new site file.
	 * @param input the input file from which the file is read.
	 * @param output the output file to which the final content is written.
	 * @param metadata the metadata of the file. */
	public SiteFile (File input, File output, Map<String, Object> metadata) {
		this.input = input;
		this.output = output;
		this.content = null;
		this.metadata = metadata;
	}

	/** Creates a new site file.
	 * @param input the input file from which the file is read.
	 * @param output the output file to which the final content is written.
	 * @param content the content of the file. Text files are assumed to be UTF-8 encoded. */
	public SiteFile (File input, File output, byte[] content) {
		this.input = input;
		this.output = output;
		this.content = content;
		this.metadata = new HashMap<String, Object>();
	}

	/** Returns the output file to which the final content of the file will be written by the {@link SiteGenerator}. **/
	public File getOutput () {
		return output;
	}

	/** Returns the content of the file. Text files will be returned as UTF-8 strings. **/
	public byte[] getContent () {
		return content;
	}

	/** Sets the content of the file. Text files are assumed to be UTF-8. **/
	public void setContent (byte[] content) {
		this.content = content;
	}

	/** Returns the input file. **/
	public File getInput () {
		return input;
	}

	/** Returns the metadata found in the first template code span, or an empty map if the file has no metadata. **/
	public Map<String, Object> getMetadata () {
		return metadata;
	}

	/** Returns the output directory of this file, relative to the base output directory. E.g. if the base output directory is
	 * "output/" and the output file is <code>output/blog/index.html</code>, this method returns <code>blog/</code>. **/
	public String getOutputDirectory () {
		if (output.getParent() == null) return "";
		String url = output.getParent().indexOf(File.separator) >= 0 ? output.getParent().substring(output.getParent().indexOf(File.separator) + 1) : output.getParent();
		url = url.replace(File.separatorChar, '/');
		if (!url.endsWith("/")) url += "/";
		return url.replace("/./", "/");
	}
}
