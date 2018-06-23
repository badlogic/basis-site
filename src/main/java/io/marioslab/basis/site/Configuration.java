package io.marioslab.basis.site;

import java.io.File;

public class Configuration {
	private final File input;
	private final File output;
	private final boolean deleteOutput;
	private final boolean watch;

	public Configuration (File input, File output, boolean deleteOutput, boolean watch) {
		super();
		this.input = input;
		this.output = output;
		this.deleteOutput = deleteOutput;
		this.watch = watch;
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
}