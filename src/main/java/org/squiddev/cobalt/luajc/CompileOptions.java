package org.squiddev.cobalt.luajc;

/**
 * Various options for compilation
 */
public class CompileOptions {
	/**
	 * Default value for {@link #prefix}
	 */
	public static final String PREFIX = "org/squiddev/luaj/luajc/generated/";

	/**
	 * Default value for {@link #compileThreshold}
	 */
	public static final int THRESHOLD = 10;

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
	 * Used to handle errors on class generation
	 * When {@code null} the exception will be propagated
	 */
	public final ErrorHandler handler;

	/**
	 * Create a new compilation options
	 *
	 * @param prefix           Set {@link #prefix}. The default is {@link #PREFIX}.
	 * @param compileThreshold Set {@link #compileThreshold}. The default is {@link #THRESHOLD}.
	 * @param verify           Set {@link #verify}. The default is true.
	 * @param handler          Set {@link #handler}. The default is {@code null}.
	 */
	public CompileOptions(String prefix, int compileThreshold, boolean verify, ErrorHandler handler) {
		this.prefix = prefix;
		this.compileThreshold = compileThreshold;
		this.verify = verify;
		this.handler = handler;
		dotPrefix = prefix.replace('/', '.');
	}

	public CompileOptions() {
		this(PREFIX, THRESHOLD, true, null);
	}
}
