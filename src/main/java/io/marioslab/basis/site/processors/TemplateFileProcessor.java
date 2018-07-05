
package io.marioslab.basis.site.processors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.marioslab.basis.site.SiteFile;
import io.marioslab.basis.site.SiteFileProcessor;
import io.marioslab.basis.site.SiteGenerator;
import io.marioslab.basis.site.SiteGenerator.SiteGeneratorException;
import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader.FileTemplateLoader;
import io.marioslab.basis.template.TemplateLoader.Source;
import io.marioslab.basis.template.parsing.Ast.BinaryOperation;
import io.marioslab.basis.template.parsing.Ast.BinaryOperation.BinaryOperator;
import io.marioslab.basis.template.parsing.Ast.Node;
import io.marioslab.basis.template.parsing.Ast.Text;
import io.marioslab.basis.template.parsing.Ast.VariableAccess;
import io.marioslab.basis.template.parsing.Parser;
import io.marioslab.basis.template.parsing.Parser.Macros;
import io.marioslab.basis.template.parsing.Parser.ParserResult;

/** Processes all files that contain the string ".bt." in their file name. The file content is interpreted as a basis-template.
 * The output file name is stripped of the ".bt." infix. The {@link SiteFile} instance is passed to the template via the variable
 * <code>file</code>. */
public class TemplateFileProcessor implements SiteFileProcessor {
	private final List<FunctionProvider> functionProviders;

	/** Constructs a new processor. The {@link FunctionProvider} instances will be called on every processed template file to set
	 * variables and functions on the {@link TemplateContext} passed to the template. See {@link BuiltinFunctionProvider} for the
	 * default implementation. */
	public TemplateFileProcessor (List<FunctionProvider> functionProviders) {
		this.functionProviders = functionProviders;
	}

	@Override
	public String processOutputFileName (String fileName) {
		return fileName.replace(".bt.", ".");
	}

	@Override
	public void process (SiteFile file) {
		if (!file.getInput().getName().contains(".bt.")) return;

		Template template = loadTemplate(file.getInput().getPath(), file.getContent());

		// Read the metadata node if any.
		readMetadata(template.getNodes(), file);

		TemplateContext context = new TemplateContext();
		context.set("file", file);
		for (FunctionProvider provider : functionProviders)
			provider.provide(file, context);

		ByteArrayOutputStream newContent = new ByteArrayOutputStream(file.getContent().length);
		template.render(context, newContent);
		try {
			newContent.flush();
		} catch (IOException e) {
			// This should never happen...
		}
		file.setContent(newContent.toByteArray());
	}

	@SuppressWarnings("unchecked")
	static void readMetadata (List<Node> nodes, SiteFile file) {
		if (nodes.size() > 0) {
			for (Node node : nodes) {
				if (node instanceof Text) continue;
				if (!(node instanceof BinaryOperation)) continue;
				BinaryOperation assignment = (BinaryOperation)node;
				if (assignment.getOperator() != BinaryOperator.Assignment) continue;
				if (!(assignment.getLeftOperand() instanceof VariableAccess)) continue;
				if (!((VariableAccess)assignment.getLeftOperand()).getVariableName().getText().equals("metadata")) continue;
				TemplateContext context = new TemplateContext();
				context.set("parseDate", (Function<String, Date>) (String date) -> {
					try {
						return new SimpleDateFormat("yyyy/MM/dd hh:ss").parse(date);
					} catch (ParseException e) {
						throw new RuntimeException("Couldn't parse date " + date + ", expected format 'yyyy/mm/dd hh:ss'.");
					}
				});
				context.set("formatDate", (BiFunction<String, Date, String>) (String format, Date date) -> {
					return new SimpleDateFormat(format).format(date);
				});
				try {
					node.evaluate(new Template(Arrays.asList(node), new Macros(), Collections.emptyList()), context, new OutputStream() {
						@Override
						public void write (int b) {
						}
					});
				} catch (IOException e) {
					// never reached
				}
				if (context.get("metadata") instanceof Map) {
					Map<String, Object> metadata = (Map<String, Object>)context.get("metadata");
					for (String key : metadata.keySet()) {
						file.getMetadata().put(key, metadata.get(key));
					}
				}
				break;
			}
		}
	}

	static Template loadTemplate (String path, byte[] content) {
		// Load the template. Since we've already loaded it from disk,
		// but any includes are not loaded, we have to hack the FileTemplateLoader a little.
		return new FileTemplateLoader() {
			@Override
			protected Source loadSource (String loadPath) {
				if (loadPath.equals(path)) {
					try {
						return new Source(loadPath, new String(content, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				} else {
					return super.loadSource(loadPath);
				}
			}
		}.load(path);
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
		private final SiteGenerator siteGenerator;

		public BuiltinFunctionProvider (SiteGenerator siteGenerator) {
			this.siteGenerator = siteGenerator;
		}

		private void list (File directory, List<SiteFile> files, boolean withMetadataOnly, boolean recursive) {
			File[] children = directory.listFiles();
			if (children != null) {
				for (File child : children) {
					if (child.isFile()) {
						File outputFile = siteGenerator.generateOutputFile(child);
						if (child.getName().contains(".bt.")) {
							try {
								byte[] content = Files.readAllBytes(child.toPath());
								SiteFile siteFile = new SiteFile(child, outputFile, new HashMap<String, Object>());
								ParserResult result = new Parser().parse(new Source(child.getPath(), new String(content, "UTF-8")));
								readMetadata(result.getNodes(), siteFile);
								if (!withMetadataOnly || (withMetadataOnly && siteFile.getMetadata().size() > 0)) files.add(siteFile);
							} catch (Throwable e) {
								throw new SiteGeneratorException("Could not read metadata of file " + child.getPath() + ".", e);
							}
						} else {
							if (!withMetadataOnly) files.add(new SiteFile(child, outputFile, new HashMap<String, Object>()));
						}
					} else if (child.isDirectory() && recursive) {
						list(child, files, withMetadataOnly, recursive);
					}
				}
			}
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public void provide (SiteFile file, TemplateContext context) {
			context.set("parseDate", (Function<String, Date>) (String date) -> {
				try {
					return new SimpleDateFormat("yyyy/MM/dd hh:ss").parse(date);
				} catch (ParseException e) {
					throw new RuntimeException("Couldn't parse date " + date + ", expected format 'yyyy/mm/dd hh:ss'.");
				}
			});

			context.set("formatDate", (BiFunction<String, Date, String>) (String format, Date date) -> {
				return new SimpleDateFormat(format).format(date);
			});

			context.set("listFiles", (TriFunction<String, Boolean, Boolean, List<SiteFile>>) (String dir, Boolean withMetadataOnly, Boolean recursive) -> {
				List<SiteFile> files = new ArrayList<SiteFile>();
				File directory = new File(siteGenerator.getInputDirectory(), dir);
				list(directory, files, withMetadataOnly, recursive);
				return files;
			});

			context.set("sortFiles",
				(TriFunction<List<SiteFile>, String, Boolean, List<SiteFile>>) (List<SiteFile> files, String metadataField, Boolean ascending) -> {
					files.sort( (SiteFile a, SiteFile b) -> {
						int result = 0;
						Object valA = a.getMetadata().get(metadataField);
						Object valB = b.getMetadata().get(metadataField);
						if (valA == null && valB == null)
							return 0;
						else if (valA == null && valB != null)
							result = -1;
						else if (valA != null && valB == null)
							result = 1;
						else {
							if (valA instanceof Comparable && valB instanceof Comparable) {
								return ((Comparable)valA).compareTo(valB);
							} else {
								return 0;
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
