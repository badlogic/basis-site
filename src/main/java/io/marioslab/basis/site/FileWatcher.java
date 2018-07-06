
package io.marioslab.basis.site;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import com.sun.nio.file.SensitivityWatchEventModifier;

import io.marioslab.basis.site.SiteGenerator.SiteGeneratorException;

/** Call the {@link FileWatcher#watch(File, Runnable)} method to listen for changes (additions, deletions, modifications) of files
 * and sub-folders in a folder. **/
@SuppressWarnings("restriction")
public class FileWatcher {
	private FileWatcher () {
	}

	/** Watches a directory for changes (file/folder additions, deletion, modification). Calls the provided {@link Runnable} in
	 * case of a change. Throws a {@link SiteGeneratorException} if watching the directory failed.
	 * @param directory
	 * @param onChange */
	@SuppressWarnings("unchecked")
	public static void watch (File directory, Runnable onChange) {
		try {
			WatchService watcher = FileSystems.getDefault().newWatchService();
			Map<WatchKey, File> keys = new HashMap<>();
			registerDirectories(watcher, directory, keys);

			while (true) {
				WatchKey key = watcher.take();

				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();
					if (kind == StandardWatchEventKinds.OVERFLOW) continue;

					WatchEvent<Path> ev = (WatchEvent<Path>)event;
					Path filename = ev.context();
					File file = new File(keys.get(key), filename.toFile().getName());
					if (file.exists() && file.isDirectory()) {
						registerDirectories(watcher, file, keys);
					}
				}

				key.reset();
				onChange.run();
			}

		} catch (Throwable t) {
			throw new SiteGeneratorException("Watching directory " + directory + " for changes failed.", t);
		}
	}

	private static void registerDirectories (WatchService watcher, File dir, Map<WatchKey, File> keys) throws IOException {
		if (!dir.isDirectory()) return;
		keys.put(
			dir.toPath().register(watcher, new WatchEvent.Kind[] {StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY}, SensitivityWatchEventModifier.HIGH),
			dir);

		File[] children = dir.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory()) registerDirectories(watcher, child, keys);
			}
		}
	}
}
