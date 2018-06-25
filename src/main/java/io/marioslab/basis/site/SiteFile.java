
package io.marioslab.basis.site;

import java.util.Map;

public class SiteFile {
	public final String inputDirectory;
	public final String inputName;
	public final String outputDirectory;
	public final String outputName;
	public final boolean isDir;
	public final Map<String, Object> metadata;

	public SiteFile (String inputDirectory, String inputName, String outputDirectory, String outputName, boolean isDir, Map<String, Object> metadata) {
		super();
		this.inputDirectory = inputDirectory;
		this.inputName = inputName;
		this.outputDirectory = inputDirectory;
		this.outputName = outputName;
		this.isDir = isDir;
		this.metadata = metadata;
	}
}
