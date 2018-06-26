
package io.marioslab.basis.site;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** A file to be processed by a {@link SiteGenerator}. **/
public class SiteFile {
	public final File input;
	public File output;
	public byte[] content;
	public final Map<String, Object> metadata;

	public SiteFile (File input, File output, byte[] content) {
		this.input = input;
		this.output = output;
		this.content = content;
		this.metadata = new HashMap<String, Object>();
	}

	/** Returns a URL based on the current path in the output field. This can be used in templates to set links. **/
	public String getUrl () {
		if (output.getParent() == null) return "";
		String url = output.getParent().indexOf('/') >= 0 ? output.getParent().substring(output.getParent().indexOf('/') + 1) : output.getParent();
		return url.replace("/./", "/");
	}
}
