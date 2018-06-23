
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

	public static Configuration parse (String[] args) {
		File input = null;
		File output = null;
		boolean deleteOutput = false;
		boolean watch = false;

		int i = 0;
		while (i < args.length) {
			String arg = args[i];

			if (arg.equals("-i")) {
				i++;
				if (args.length == i) BasisSite.error("Expected an input directory");
				input = new File(args[i]);
			} else if (arg.equals("-o")) {
				i++;
				if (args.length == i) BasisSite.error("Expected an output directory");
				output = new File(args[i]);
			} else if (arg.equals("-w")) {
				watch = true;
			} else if (arg.equals("-d")) {
				deleteOutput = true;
			} else {
				BasisSite.error("Unknown argument '" + arg + "'");
			}
			i++;
		}

		if (input == null) BasisSite.error("Expected an input directory.");
		if (output == null) BasisSite.error("Expected an output directory.");

		return new Configuration(input, output, deleteOutput, watch);
	}
}
