
package io.marioslab.basis.site;

import io.marioslab.basis.site.SiteGenerator.SiteGeneratorException;

/** A file processor receives an input file, the output file name, the file's content, and its optional metadata as a
 * {@link SiteFile} instance. Based on this data, the file processor may modify the output file name, the content, and metadata. A
 * file processor is registered with a {@link SiteGenerator} instance via the {@link SiteGenerator#getProcessors()} method, which
 * provides the processor with the data. */
public interface SiteFileProcessor {
	/** Processes the file, e.g modify its content and (optional) metadata. May not modify the file, e.g. because the file type
	 * can't be processed. Throws a {@link SiteGeneratorException} if processing a supprted file type failed. **/
	public void process (SiteFile file);

	/** Transforms the output file name, e.g. stripping parts, and returns the modified name. If no modification is performed, the
	 * input is returned. **/
	public String processOutputFileName (String fileName);
}
