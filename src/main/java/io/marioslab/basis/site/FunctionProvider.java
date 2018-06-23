
package io.marioslab.basis.site;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import io.marioslab.basis.template.TemplateContext;

/** Provides functions to a template by setting them on a {@link TemplateContext}. You can specify providers for site generation
 * via the {@link Configuration} class. **/
public interface FunctionProvider {

	@FunctionalInterface
	public interface TriFunction<S, T, U, R> {
		R apply (S s, T t, U u);
	}

	@FunctionalInterface
	public interface QuadFunction<S, T, U, V, R> {
		R apply (S s, T t, U u, V v);
	}

	public void provide (File input, File output, Configuration config, TemplateContext context);

	public static class BuiltinFunctionProvider implements FunctionProvider {
		@SuppressWarnings("unchecked")
		@Override
		public void provide (File input, File output, Configuration config, TemplateContext context) {
			context.set("formatDate", (BiFunction<String, Date, String>) (String format, Date date) -> {
				return new SimpleDateFormat(format).format(date);
			});

			context.set("listFiles", (BiFunction<String, Boolean, List<SiteFile>>) (String dir, Boolean withMetadataOnly) -> {
				List<SiteFile> files = new ArrayList<SiteFile>();
				File directory = new File(dir);
				File[] children = directory.listFiles();
				if (children != null) {
					for (File child : children) {
						Map<String, Object> metadata = FileUtils.readMetadataBlock(child);

						if ((withMetadataOnly && metadata != null) || !withMetadataOnly) {
							File childOutput = new File(config.getOutput(),
								child.getAbsolutePath().replace(".bt.", ".").replace(config.getInput().getAbsolutePath(), ""));
							files.add(new SiteFile(child.getParent(), child.getName(), childOutput.getParent(), childOutput.getName(), child.isDirectory(), metadata));
						}
					}
				}
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
