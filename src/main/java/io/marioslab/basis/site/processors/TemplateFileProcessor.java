
package io.marioslab.basis.site.processors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import io.marioslab.basis.site.SiteFile;
import io.marioslab.basis.site.SiteFileProcessor;
import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader.FileTemplateLoader;

/** Processes all files that contain the string ".bt." in their file name. The file content is interpreted as a basis-template.
 * The output file name is stripped of the ".bt." infix. The {@link SiteFile} instance is passed to the template via the variable
 * <code>file</code>. */
public class TemplateFileProcessor implements SiteFileProcessor {
	private final List<FunctionProvider> functionProviders;

	/** Constructs a new processor. The {@link FunctionProvider} instances will be called on every processed template file to set
	 * variables and functions on the {@link TemplateContext} passed to the template. */
	public TemplateFileProcessor (List<FunctionProvider> functionProviders) {
		this.functionProviders = functionProviders;
	}

	@Override
	public void process (SiteFile file) {
		if (!file.input.getName().contains(".bt.")) return;

		// Strip the output file name of the ".bt" infix
		file.output = new File(file.output.getPath().replace(".bt.", ""));

		// Load the template. Since we've already loaded it from disk,
		// but any includes are not loaded, we have to hack the FileTemplateLoader a little.
		Template template = null;
		template = new FileTemplateLoader() {
			@Override
			protected Source loadSource (String path) {
				if (path.equals(file.input.getPath())) {
					try {
						return new Source(path, new String(file.content, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				} else {
					return super.loadSource(path);
				}
			}
		}.load(file.input.getPath());

		TemplateContext context = new TemplateContext();
		context.set("file", file);
		for (FunctionProvider provider : functionProviders)
			provider.provide(file, context);

		ByteArrayOutputStream newContent = new ByteArrayOutputStream(file.content.length);
		template.render(context, newContent);
		try {
			newContent.flush();
		} catch (IOException e) {
			// This should never happen...
		}
		file.content = newContent.toByteArray();
	}

	/** Provides functions to a template by setting them on a {@link TemplateContext}. Used by a {@link TemplateFileProcessor} to
	 * expose functionality to templated files. **/
	public interface FunctionProvider {

		@FunctionalInterface
		public interface VoidFunction<S> {
			void apply (S s);
		}

		@FunctionalInterface
		public interface TriFunction<S, T, U, R> {
			R apply (S s, T t, U u);
		}

		@FunctionalInterface
		public interface QuadFunction<S, T, U, V, R> {
			R apply (S s, T t, U u, V v);
		}

		public void provide (SiteFile file, TemplateContext context);
	}

	public static class BuiltinFunctionProvider implements FunctionProvider {
		private final File inputDirectory;
		private final File outputDirectory;

		public BuiltinFunctionProvider (File inputDirectory, File outputDirectory) {
			this.inputDirectory = inputDirectory;
			this.outputDirectory = outputDirectory;
		}

		private void list (File directory, List<SiteFile> files, boolean withMetadataOnly, boolean recursive) {
			File[] children = directory.listFiles();
			if (children != null) {
				for (File child : children) {
					if (child.isFile()) {
						try (InputStream in = new FileInputStream(child)) {
							Map<String, Object> metadata = MetadataProcessor.readMetadataBlock(in);
							if ((withMetadataOnly && metadata != null) || !withMetadataOnly) {
								File output = new File(outputDirectory, child.getAbsolutePath().replace(".bt.", ".").replace(inputDirectory.getAbsolutePath(), ""));
								SiteFile file = new SiteFile(child, output, new byte[0]);
								files.add(file);
							}
						} catch (FileNotFoundException e) {
							throw new RuntimeException("Couldn't open file " + child.getPath() + ".", e);
						} catch (IOException e) {
							throw new RuntimeException("Couldn't open file " + child.getPath() + ".", e);
						}
					} else if (child.isDirectory() && recursive) {
						list(child, files, withMetadataOnly, recursive);
					}
				}
			}
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void provide (SiteFile file, TemplateContext context) {
			context.set("formatDate", (BiFunction<String, Date, String>) (String format, Date date) -> {
				return new SimpleDateFormat(format).format(date);
			});

			context.set("listFiles", (TriFunction<String, Boolean, Boolean, List<SiteFile>>) (String dir, Boolean withMetadataOnly, Boolean recursive) -> {
				List<SiteFile> files = new ArrayList<SiteFile>();
				File directory = new File(inputDirectory.getParentFile(), dir);
				list(directory, files, withMetadataOnly, recursive);
				return files;
			});

			context.set("sortFiles",
				(TriFunction<List<SiteFile>, String, Boolean, List<SiteFile>>) (List<SiteFile> files, String metadataField, Boolean ascending) -> {
					files.sort( (SiteFile a, SiteFile b) -> {
						int result = 0;
						Object valA = a.metadata != null ? a.metadata.get(metadataField) : null;
						Object valB = b.metadata != null ? b.metadata.get(metadataField) : null;
						if (valA == null && valB == null)
							return 0;
						else if (valA == null && valB != null)
							result = -1;
						else if (valA != null && valB == null)
							result = 1;
						else {
							if (valA instanceof Integer && valB instanceof Integer) {
								result = ((Integer)valA).compareTo((Integer)valB);
							} else if (valA instanceof Float && valB instanceof Float) {
								result = ((Float)valA).compareTo((Float)valB);
							} else if (valA instanceof Boolean && valB instanceof Boolean) {
								result = ((Boolean)valA).compareTo((Boolean)valB);
							} else if (valA instanceof Date && valB instanceof Date) {
								result = ((Date)valA).compareTo((Date)valB);
							} else if (valA instanceof String && valB instanceof String) {
								result = ((String)valA).compareTo((String)valB);
							} else {
								result = 0;
							}
						}
						return ascending ? result : -result;
					});
					return files;
				});

			context.set("sort", (BiFunction<Object, Boolean, Object>) (listOrArrayOrMap, ascending) -> {
				if (listOrArrayOrMap instanceof List) {
					Collections.sort((List)listOrArrayOrMap);
					if (!ascending) Collections.reverse((List)listOrArrayOrMap);
				} else if (listOrArrayOrMap instanceof boolean[]) {
					boolean[] array = (boolean[])listOrArrayOrMap;
					Boolean[] objectArray = new Boolean[array.length];
					for (int i = 0, n = objectArray.length; i < n; i++)
						objectArray[i] = array[i];
					Arrays.sort(objectArray, (Boolean a, Boolean b) -> {
						return ascending ? a.compareTo(b) : b.compareTo(a);
					});
					for (int i = 0, n = array.length; i < n; i++)
						array[i] = objectArray[i];
				} else if (listOrArrayOrMap instanceof char[]) {
					char[] array = (char[])listOrArrayOrMap;
					Arrays.sort(array);
					if (!ascending) {
						for (int i = 0, n = array.length; i < n / 2; i++) {
							char temp = array[i];
							array[i] = array[array.length - 1 - i];
							array[array.length - 1 - i] = temp;
						}
					}
				} else if (listOrArrayOrMap instanceof byte[]) {
					byte[] array = (byte[])listOrArrayOrMap;
					Arrays.sort(array);
					if (!ascending) {
						for (int i = 0, n = array.length; i < n / 2; i++) {
							byte temp = array[i];
							array[i] = array[array.length - 1 - i];
							array[array.length - 1 - i] = temp;
						}
					}
				} else if (listOrArrayOrMap instanceof short[]) {
					short[] array = (short[])listOrArrayOrMap;
					Arrays.sort(array);
					if (!ascending) {
						for (int i = 0, n = array.length; i < n / 2; i++) {
							short temp = array[i];
							array[i] = array[array.length - 1 - i];
							array[array.length - 1 - i] = temp;
						}
					}
				} else if (listOrArrayOrMap instanceof int[]) {
					int[] array = (int[])listOrArrayOrMap;
					Arrays.sort(array);
					if (!ascending) {
						for (int i = 0, n = array.length; i < n / 2; i++) {
							int temp = array[i];
							array[i] = array[array.length - 1 - i];
							array[array.length - 1 - i] = temp;
						}
					}
				} else if (listOrArrayOrMap instanceof long[]) {
					long[] array = (long[])listOrArrayOrMap;
					Arrays.sort(array);
					if (!ascending) {
						for (int i = 0, n = array.length; i < n / 2; i++) {
							long temp = array[i];
							array[i] = array[array.length - 1 - i];
							array[array.length - 1 - i] = temp;
						}
					}
				} else if (listOrArrayOrMap instanceof float[]) {
					float[] array = (float[])listOrArrayOrMap;
					Arrays.sort(array);
					if (!ascending) {
						for (int i = 0, n = array.length; i < n / 2; i++) {
							float temp = array[i];
							array[i] = array[array.length - 1 - i];
							array[array.length - 1 - i] = temp;
						}
					}
				} else if (listOrArrayOrMap instanceof double[]) {
					double[] array = (double[])listOrArrayOrMap;
					Arrays.sort(array);
					if (!ascending) {
						for (int i = 0, n = array.length; i < n / 2; i++) {
							double temp = array[i];
							array[i] = array[array.length - 1 - i];
							array[array.length - 1 - i] = temp;
						}
					}
				} else if (listOrArrayOrMap instanceof Object[]) {
					Object[] array = (Object[])listOrArrayOrMap;
					Arrays.sort(array);
					if (!ascending) {
						for (int i = 0, n = array.length; i < n / 2; i++) {
							Object temp = array[i];
							array[i] = array[array.length - 1 - i];
							array[array.length - 1 - i] = temp;
						}
					}
				}

				return listOrArrayOrMap;
			});
		}
	}
}
