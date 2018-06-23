
package io.marioslab.basis.site;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class FileUtils {
	public static void delete (File file) {
		if (!file.exists()) return;

		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children == null) throw new RuntimeException("Could not read files in directory " + file.getPath());
			for (File child : children) {
				delete(child);
			}
			if (!file.delete()) throw new RuntimeException("Could not delete directory " + file.getPath());
		} else {
			if (!file.delete()) throw new RuntimeException("Could not delete file " + file.getPath());
		}
	}

	public static void writeFile (String content, File output) {
		try {
			Files.write(output.toPath(), content.getBytes("UTF-8"));
		} catch (IOException e) {
			throw new RuntimeException("Couldn't write to file " + output.getPath() + ".", e);
		}
	}

	public static String stripMetadataBlock (File input) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (BufferedReader reader = new BufferedReader(new FileReader(input)); BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
			String line = reader.readLine();
			if (line.equals("+++")) {
				// strip metadata block
				while (!"+++".equals((line = reader.readLine())))
					;
			}

			line = reader.readLine();
			while (line != null) {
				writer.write(line);
				line = reader.readLine();
				if (line != null) writer.write("\n");
			}

			writer.flush();
			return new String(out.toByteArray());
		} catch (Throwable t) {
			throw new RuntimeException(t);
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
	 * Values are converted to booleans, ints, floats, dates (format yyyy/mm/dd hh:mm:ss, or yyyy/mm/dd) or strings if possible.
	 * </p>
	 *
	 * <p>
	 * The key/value pairs will be returned in a <code>Map&lt;String, Object&gt;</code>. If no meta data block was found, null is
	 * returned.
	 * </p>
	 */
	public static Map<String, Object> readMetadataBlock (File file) {
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/mm/dd hh:mm:ss");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/mm/dd");

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line = reader.readLine();
			if (!line.equals("+++")) return null;

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
