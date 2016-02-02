package org.squiddev.luaj.luajc;

/**
 * Various options for compilation
 */
public class CompileOptions {
	/**
	 * The prefix for all classes in slash form
	 */
	public final String prefix;

	/**
	 * The prefix for all classes in dot form
	 */
	public final String dotPrefix;

	/**
	 * Number of calls before compiling:
	 * 1 compiles when first called,
	 * 0 or less compiles when loaded
	 */
	public final int compileThreshold;

	/**
	 * Verify the compiled sources
	 * This helps debug but will slow down compilation massively
	 */
	public final boolean verify;

	/**
	 * Create a new compilation options
	 *
	 * @param prefix           Set {@link #prefix}. The default is {@link Constants#PREFIX}.
	 * @param compileThreshold Set {@link #compileThreshold}. The default is 10.
	 * @param verify           Set {@link #verify}. The default is true.
	 */
	public CompileOptions(String prefix, int compileThreshold, boolean verify) {
		this.prefix = prefix;
		this.compileThreshold = compileThreshold;
		this.verify = verify;
		dotPrefix = prefix.replace('/', '.');
	}

	public CompileOptions() {
		this(Constants.PREFIX, 10, true);
	}
}
