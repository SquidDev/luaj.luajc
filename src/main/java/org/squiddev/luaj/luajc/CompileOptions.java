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
	public static final int THRESHOLD = 10;

	/**
	 * Default value for {@link #maximumCount}
	 */
	public static final int MAXIMUM_COUNT = 2000;

	/**
	 * Default value for {@link #threadedThreshold}
	 */
	public static final int THREADED_THRESHOLD = 500;

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
	 * Maximum number of instructions that can be compiled.
	 * This is to prevent there being more than 16384 (64kb) Java instructions.
	 * Set to 0 or less to have no maximum.
	 */
	public final int maximumCount;

	/**
	 * Number of instructions before compilation is delegated to a thread.
	 * This prevents delays when compiling.
	 * Set to 0 or less to never delegate in a thread.
	 */
	public final int threadedThreshold;

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
		this.maximumCount = MAXIMUM_COUNT;
		this.threadedThreshold = THREADED_THRESHOLD;
		this.verify = verify;
		this.handler = handler;
		dotPrefix = prefix.replace('/', '.');
	}


	/**
	 * Create a new compilation options
	 *
	 * @param prefix            Set {@link #prefix}. The default is {@link #PREFIX}.
	 * @param compileThreshold  Set {@link #compileThreshold}. The default is {@link #THRESHOLD}.
	 * @param maximumCount      Set {@link #maximumCount}. The default is {@link #MAXIMUM_COUNT}
	 * @param threadedThreshold Set {@link #threadedThreshold}. The default is {@link #THREADED_THRESHOLD}.
	 * @param verify            Set {@link #verify}. The default is true.
	 * @param handler           Set {@link #handler}. The default is {@code null}.
	 */
	public CompileOptions(String prefix, int compileThreshold, int maximumCount, int threadedThreshold, boolean verify, ErrorHandler handler) {
		this.prefix = prefix;
		this.compileThreshold = compileThreshold;
		this.maximumCount = maximumCount;
		this.threadedThreshold = threadedThreshold;
		this.verify = verify;
		this.handler = handler;
		dotPrefix = prefix.replace('/', '.');
	}

	public CompileOptions() {
		this(PREFIX, THRESHOLD, MAXIMUM_COUNT, THREADED_THRESHOLD, true, null);
	}
}
