package org.squiddev.luaj.luajc.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * An exception when validating a class
 */
public class ValidationException extends RuntimeException {
	private final ClassReader reader;
	private final String trace;

	public ValidationException(ClassReader reader, String trace) {
		super("Generation error for " + reader.getClassName());
		this.reader = reader;
		this.trace = trace;
	}

	public ValidationException(ClassReader reader, String trace, Throwable cause) {
		super("Generation error for " + reader.getClassName(), cause);
		this.reader = reader;
		this.trace = trace;
	}

	public final String getTrace() {
		return trace;
	}

	public final ClassReader getReader() {
		return reader;
	}

	public final String getClassDump() {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		reader.accept(new TraceClassVisitor(printWriter), 0);

		return writer.toString();
	}
}
