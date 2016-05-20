package org.squiddev.cobalt.luajc;

import org.squiddev.cobalt.luajc.analysis.ProtoInfo;

/**
 * A handler for generation errors in classes.
 *
 * @see CompileOptions#handler
 */
public interface ErrorHandler {
	/**
	 * Handle an error in generating a class
	 *
	 * @param info      The prototype we are generating for
	 * @param throwable The thrown exception. This will be an instance of {@link VerifyError} or {@link Exception}.
	 */
	void handleError(ProtoInfo info, Throwable throwable);
}
