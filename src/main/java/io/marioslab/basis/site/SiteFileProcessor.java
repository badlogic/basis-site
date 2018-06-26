
package io.marioslab.basis.site;

/** A file processor receives an input file, the output file destination and the file's content and metadata and may decide to
 * ignore the file or modify its content. A file processor is registered with a {@link BasisSite} instance which provides the
 * processor with the data. */
public interface SiteFileProcessor {
	public void process (SiteFile file);
}
