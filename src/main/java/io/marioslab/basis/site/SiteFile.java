
package io.marioslab.basis.site;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** A file to be processed by a {@link SiteGenerator}. **/
public class SiteFile {
	private final File input;
	private File output;
	private byte[] content;
	private final Map<String, Object> metadata;

	public SiteFile (File input, File output, Map<String, Object> metadata) {
		this.input = input;
		this.output = output;
		this.content = null;
		this.metadata = metadata;
	}

	public SiteFile (File input, File output, byte[] content) {
		this.input = input;
		this.output = output;
		this.content = content;
		this.metadata = new HashMap<String, Object>();
	}

	/** Returns the output file name to which the final content of the file will be written by the {@link SiteGenerator}. **/
	public File getOutput () {
		return output;
	}

	/** Returns the content of the file. **/
	public byte[] getContent () {
		return content;
	}

	/** Sets the content of the file. **/
	public void setContent (byte[] content) {
		this.content = content;
	}

	/** Returns the input file name. **/
	public File getInput () {
		return input;
	}

	/** Returns the metadata. **/
	public Map<String, Object> getMetadata () {
		return metadata;
	}

	/** Returns a URL based on the current path in the output field. This can be used in templates to set links. **/
	public String getUrl () {
		if (output.getParent() == null) return "";
		String url = output.getParent().indexOf('/') >= 0 ? output.getParent().substring(output.getParent().indexOf('/') + 1) : output.getParent();
		return url.replace("/./", "/");
	}
}
