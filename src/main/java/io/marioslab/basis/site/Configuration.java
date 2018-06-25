
package io.marioslab.basis.site;

import java.io.File;

import io.marioslab.basis.site.FunctionProvider.BuiltinFunctionProvider;

public class Configuration {
	private final File input;
	private final File output;
	private final boolean deleteOutput;
	private final boolean watch;
	private final FunctionProvider[] providers;

	public Configuration (File input, File output, boolean deleteOutput, boolean watch, FunctionProvider... providers) {
		this.input = input;
		this.output = output;
		this.deleteOutput = deleteOutput;
		this.watch = watch;
		if (providers.length == 0) {
			this.providers = new FunctionProvider[] {new BuiltinFunctionProvider()};
		} else {
			this.providers = providers;
		}
	}

	public File getInput () {
		return input;
	}

	public File getOutput () {
		return output;
	}

	public boolean isDeleteOutput () {
		return deleteOutput;
	}

	public boolean isWatch () {
		return watch;
	}

	public FunctionProvider[] getFunctionProviders () {
		return providers;
	}

	public static Configuration parse (String[] args, ConfigurationExtension... extensions) {
		File input = null;
		File output = null;
		boolean deleteOutput = false;
		boolean watch = false;

		int i = 0;
		while (i < args.length) {
			String arg = args[i];

			if (arg.equals("-i")) {
				i++;
				if (args.length == i) BasisSite.fatalError("Expected an input directory", true, extensions);
				input = new File(args[i]);
			} else if (arg.equals("-o")) {
				i++;
				if (args.length == i) BasisSite.fatalError("Expected an output directory", true, extensions);
				output = new File(args[i]);
			} else if (arg.equals("-w")) {
				watch = true;
			} else if (arg.equals("-d")) {
				deleteOutput = true;
			} else {
				boolean parsed = false;
				for (ConfigurationExtension ext : extensions) {
					int result = ext.parseArgument(args, i);
					if (result > -1) {
						i = result;
						parsed = true;
						break;
					}
				}
				if (!parsed) {
					BasisSite.fatalError("Unknown argument '" + arg + "'", true);
				}
			}
			i++;
		}

		if (input == null) BasisSite.fatalError("Expected an input directory.", true, extensions);
		if (output == null) BasisSite.fatalError("Expected an output directory.", true, extensions);

		for (ConfigurationExtension ext : extensions) {
			ext.validate();
		}

		return new Configuration(input, output, deleteOutput, watch);
	}

	public interface ConfigurationExtension {
		public int parseArgument (String[] args, int index);

		public void validate ();

		public void printHelp ();
	}
}
