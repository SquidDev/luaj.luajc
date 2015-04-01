package org.squiddev.luaj.luajc.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.objectweb.asm.Opcodes.*;

/**
 * Utilities for writing Java bytecode
 */
public class AsmUtils {
	/**
	 * Get the appropriate constant opcode
	 *
	 * @param number The opcode number
	 * @return ICONST_n
	 */
	public static int getConstOpcode(int number) {
		return ICONST_0 + number;
	}

	/**
	 * Insert the correct Opcode for Java constants
	 *
	 * @param mv     The {@link MethodVisitor}
	 * @param number The constant to insert
	 */
	public static void constantOpcode(MethodVisitor mv, int number) {
		if (number >= -1 && number <= 5) {
			mv.visitInsn(getConstOpcode(number));
		} else if (number >= -128 && number <= 127) {
			mv.visitIntInsn(BIPUSH, number);
		} else if (number >= -32768 && number <= 32767) {
			mv.visitIntInsn(SIPUSH, (short) number);
		} else {
			mv.visitLdcInsn(number);
		}
	}

	public static void constantOpcode(MethodVisitor mv, double number) {
		if (number == 0.0D) {
			mv.visitInsn(DCONST_0);
		} else if (number == 1.0D) {
			mv.visitInsn(DCONST_1);
		} else {
			mv.visitLdcInsn(number);
		}
	}

	/**
	 * Validate a generated class
	 *
	 * @param reader The class to read
	 * @param loader The appropriate class loader
	 * @throws RuntimeException On bad validation
	 */
	public static void validateClass(ClassReader reader, ClassLoader loader) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);

		Exception error = null;
		try {
			CheckClassAdapter.verify(reader, loader, false, printWriter);
		} catch (Exception e) {
			error = e;
		}

		String contents = writer.toString();
		if (error != null || contents.length() > 0) {
			reader.accept(new TraceClassVisitor(printWriter), 0);
			throw new RuntimeException("Generation error\nDump for " + reader.getClassName() + "\n" + contents, error);
		}
	}

	/**
	 * Validate a generated class
	 *
	 * @param bytes  The class bytes to read
	 * @param loader The appropriate class loader
	 * @see #validateClass(ClassReader, ClassLoader)
	 */
	public static void validateClass(byte[] bytes, ClassLoader loader) {
		validateClass(new ClassReader(bytes), loader);
	}

}
