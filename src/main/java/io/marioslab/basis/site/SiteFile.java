
package io.marioslab.basis.site;

import java.util.Map;

public class SiteFile {
	public final String inputPath;
	public final String inputName;
	public final String outputPath;
	public final String outputName;
	public final boolean isDir;
	public final Map<String, Object> metadata;

	public SiteFile (String inputPath, String inputName, String outputPath, String outputName, boolean isDir, Map<String, Object> metadata) {
		super();
		this.inputPath = inputPath;
		this.inputName = inputName;
		this.outputPath = outputPath;
		this.outputName = outputName;
		this.isDir = isDir;
		this.metadata = metadata;
	}
}
