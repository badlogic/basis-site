
package io.marioslab.basis.site;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import com.sun.nio.file.SensitivityWatchEventModifier;

import io.marioslab.basis.site.SiteGenerator.SiteGeneratorException;

@SuppressWarnings("restriction")
public class FileWatcher {
	private FileWatcher () {
	}

	@SuppressWarnings("unchecked")
	public static void watch (File directory, Runnable onChange) {
		try {
			WatchService watcher = FileSystems.getDefault().newWatchService();
			registerDirectories(watcher, directory);

			while (true) {
				WatchKey key = watcher.take();

				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();
					if (kind == StandardWatchEventKinds.OVERFLOW) continue;

					WatchEvent<Path> ev = (WatchEvent<Path>)event;
					Path filename = ev.context();
					File file = filename.toFile();
					if (file.exists() && file.isDirectory()) {
						registerDirectories(watcher, file);
					}
				}

				if (!key.reset()) {
					throw new SiteGeneratorException("Watching directory " + directory + " for changes failed.");
				}

				onChange.run();
			}

		} catch (IOException | InterruptedException e) {
			throw new SiteGeneratorException("Watching directory " + directory + " for changes failed.", e);
		}
	}

	private static void registerDirectories (WatchService watcher, File dir) throws IOException {
		if (!dir.isDirectory()) return;
		dir.toPath().register(watcher,
			new WatchEvent.Kind[] {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY},
			SensitivityWatchEventModifier.HIGH);

		File[] children = dir.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory()) registerDirectories(watcher, child);
			}
		}
	}
}
