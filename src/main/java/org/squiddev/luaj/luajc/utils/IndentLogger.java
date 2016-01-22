package org.squiddev.luaj.luajc.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A basic logger for indenting
 */
public final class IndentLogger extends PrintWriter {
	public static final IndentLogger output = new IndentLogger(System.out, true);

	private int indent = 0;
	private boolean empty = true;

	public IndentLogger(Writer out) {
		super(out);
	}

	public IndentLogger(Writer out, boolean autoFlush) {
		super(out, autoFlush);
	}

	public IndentLogger(OutputStream out) {
		super(out);
	}

	public IndentLogger(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}

	public void indent() {
		indent++;
	}

	public void unindent() {
		indent--;
	}

	@Override
	public void println() {
		empty = true;
		super.println();
	}

	private void ensureOpen() throws IOException {
		if (out == null) throw new IOException("Stream closed");
	}

	private void writeIndent() {
		try {
			if (empty) {
				empty = false;
				for (int i = 0; i < indent; i++) out.write('\t');
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(int c) {
		try {
			ensureOpen();
			writeIndent();
			out.write(c);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(char buf[], int off, int len) {
		try {
			ensureOpen();
			if (len > 0) writeIndent();
			out.write(buf, off, len);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(String s, int off, int len) {
		try {
			ensureOpen();
			if (len > 0) writeIndent();
			out.write(s, off, len);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
