
package io.marioslab.basis.site;

import java.io.File;

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
}
