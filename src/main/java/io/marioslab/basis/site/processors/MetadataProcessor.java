
package io.marioslab.basis.site.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import io.marioslab.basis.site.SiteFile;
import io.marioslab.basis.site.SiteFileProcessor;
import io.marioslab.basis.site.SiteGenerator.SiteGeneratorException;

/**
 * <p>
 * Checks if the file starts with a metadata block, reads the metadata and stores it in the SiteFile, and replaces the current
 * content of the SiteFile with content stripped of the metadata block. The stripped block is replaced with new lines to retain
 * line numbers. See {@link #readMetadataBlock(byte[])} for a definition of metadata. */
public class MetadataProcessor implements SiteFileProcessor {
	@Override
	public void process (SiteFile file) {
		Map<String, Object> metadata = readMetadataBlock(new ByteArrayInputStream(file.content));
		if (metadata == null) return;

		for (String key : metadata.keySet())
			file.metadata.put(key, metadata.get(key));

		file.content = stripMetadataBlock(file);
	}

	/** Stripes the metadata block from the site file's content and replaces it with empty lines to retain line numbers. **/
	public static byte[] stripMetadataBlock (SiteFile file) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(file.content)));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))) {

			// strip metadata block and replace it with new lines
			String line = reader.readLine();
			if (line.equals("+++")) {
				while (!"+++".equals((line = reader.readLine())))
					writer.write("\n");
				writer.write("\n");
			}

			// write out the remaining content
			line = reader.readLine();
			while (line != null) {
				writer.write(line);
				line = reader.readLine();
				if (line != null) writer.write("\n");
			}

			writer.flush();
			return out.toByteArray();
		} catch (Throwable t) {
			throw new SiteGeneratorException("Couldn't strip metadata block from file " + file.input.getPath() + ".", t);
		}
	}

	/**
	 * <p>
	 * Reads the meta data block of a file. A meta data block starts with "+++" on the first line and is closed with "+++" on the
	 * last line of the block. Inside a block, a single line is expected to consist of a key/value pair in the format "key: value".
	 * Empty lines are also valid and will be ignored.
	 * </p>
	 *
	 * <p>
	 * Values are converted to booleans, ints, floats, dates (format "yyyy/mm/dd hh:mm:ss" or "yyyy/mm/dd") or strings if possible.
	 * </p>
	 *
	 * <p>
	 * The key/value pairs will be returned in a <code>Map&lt;String, Object&gt;</code>. If no meta data block was found, null is
	 * returned.
	 * </p>
	 */
	public static Map<String, Object> readMetadataBlock (InputStream in) {
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

			if (reader.read() != '+') return null;
			if (reader.read() != '+') return null;
			if (reader.read() != '+') return null;

			String line = reader.readLine();
			if (line == null) return null;

			// we are likely in a meta data block, read the key value pairs
			Map<String, Object> metadata = new HashMap<String, Object>();
			while (!"+++".equals((line = reader.readLine()))) {
				line = line.trim();
				if (line.isEmpty()) continue;
				int idx = line.indexOf(":");
				if (idx == -1) continue;
				String key = line.substring(0, idx).trim();
				String value = line.substring(idx + 1).trim();

				if ("true".equals(value) || "false".equals(value)) {
					metadata.put(key, Boolean.parseBoolean(value));
					continue;
				}

				try {
					metadata.put(key, Integer.parseInt(value));
					continue;
				} catch (Throwable t) {
				}

				try {
					metadata.put(key, Float.parseFloat(value));
					continue;
				} catch (Throwable t) {
				}

				try {
					metadata.put(key, dateTimeFormat.parse(value));
					continue;
				} catch (Throwable t) {
				}

				try {
					metadata.put(key, dateFormat.parse(value));
					continue;
				} catch (Throwable t) {
				}

				metadata.put(key, value);
			}
			return metadata;
		} catch (Throwable t) {
			return null;
		}
	}
}
