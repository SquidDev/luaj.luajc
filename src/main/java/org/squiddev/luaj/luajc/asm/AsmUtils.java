package org.squiddev.luaj.luajc.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.objectweb.asm.Opcodes.*;

/**
 * Utilities for writing asm
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
	 * Get the method signature
	 *
	 * @param classObj   The class to find it from
	 * @param methodName The method name
	 * @param args       Argument types
	 * @return The method signature or {@code null} or failure
	 */
	public static String getMethodDecriptor(Class<?> classObj, String methodName, Class<?>... args) {
		try {
			return Type.getMethodDescriptor(classObj.getMethod(methodName, args));
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static void validateClass(ClassReader reader, ClassLoader loader) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);

		try {
			CheckClassAdapter.verify(reader, loader, false, printWriter);
		} catch (Exception e) {
			e.printStackTrace(printWriter);
		}

		String contents = writer.toString();
		if (contents.length() > 0) {
			reader.accept(new TraceClassVisitor(printWriter), 0);
			System.out.println("Dump for " + reader.getClassName());
			System.out.println(writer);
			throw new RuntimeException("Generation error");
		}
	}

	public static void validateClass(byte[] bytes, ClassLoader loader) {
		validateClass(new ClassReader(bytes), loader);
	}

	public static void validateClass(byte[] bytes) {
		validateClass(new ClassReader(bytes), null);
	}

	public static void validateClass(ClassWriter writer, ClassLoader loader) {
		validateClass(writer.toByteArray(), loader);
	}

	/**
	 * Stores very basic data about a method so we can inject it
	 */
	public static class TinyMethod {
		public final String className;
		public final String name;
		public final String signature;

		public final Boolean isStatic;

		public TinyMethod(String className, String name, String signature, boolean isStatic) {
			this.className = className;
			this.name = name;
			this.signature = signature;
			this.isStatic = isStatic;
		}

		public TinyMethod(String className, String name, String signature) {
			this(className, name, signature, false);
		}

		public TinyMethod(Method m) {
			this(Type.getInternalName(m.getDeclaringClass()), m.getName(), Type.getMethodDescriptor(m), Modifier.isStatic(m.getModifiers()));
		}

		public static TinyMethod tryConstruct(Class<?> classObj, String methodName, Class<?>... args) {
			try {
				return new TinyMethod(classObj.getMethod(methodName, args));
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(e);
			}
		}

		public void inject(MethodVisitor mv, int opcode) {
			mv.visitMethodInsn(opcode, className, name, signature, false);
		}

		public void inject(MethodVisitor mv) {
			inject(mv, isStatic ? INVOKESTATIC : INVOKEVIRTUAL);
		}
	}
}
