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
	 * Create a new compilation options
	 *
	 * @param prefix           Set {@link #prefix}. The default is {@link Constants#PREFIX}.
	 * @param compileThreshold Set {@link #compileThreshold}. The default is 10.
	 */
	public CompileOptions(String prefix, int compileThreshold) {
		this.prefix = prefix;
		this.compileThreshold = compileThreshold;
		dotPrefix = prefix.replace('/', '.');
	}

	public CompileOptions() {
		this(Constants.PREFIX, 10);
	}
}
