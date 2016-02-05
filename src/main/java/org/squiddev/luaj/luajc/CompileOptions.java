package org.squiddev.luaj.luajc;

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
	public static final int COMPILE_THRESHOLD = 50;

	/**
	 * Default value for {@link #typeThreshold}
	 */
	public static final float TYPE_THRESHOLD = 0.7f;

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
	 * Percentage a type must be to considered
	 * worth specialising in
	 */
	public final float typeThreshold;

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
	 * @param compileThreshold Set {@link #compileThreshold}. The default is {@link #COMPILE_THRESHOLD}.
	 * @param typeThreshold    Set {@link #typeThreshold}. The default is {@link #TYPE_THRESHOLD}.
	 * @param verify           Set {@link #verify}. The default is true.
	 * @param handler          Set {@link #handler}. The default is {@code null}.
	 */
	public CompileOptions(String prefix, int compileThreshold, float typeThreshold, boolean verify, ErrorHandler handler) {
		this.prefix = prefix;
		this.compileThreshold = compileThreshold;
		this.typeThreshold = typeThreshold;
		this.verify = verify;
		this.handler = handler;
		dotPrefix = prefix.replace('/', '.');
	}

	public CompileOptions() {
		this(PREFIX, COMPILE_THRESHOLD, TYPE_THRESHOLD, true, null);
	}
}
