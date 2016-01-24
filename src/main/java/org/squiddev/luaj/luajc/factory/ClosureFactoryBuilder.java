package org.squiddev.luaj.luajc.factory;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.luajc.utils.AsmUtils;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.luajc.Constants.*;

/**
 * A class generator for closure factories
 *
 * We pass an array of upvalues, which store if the upvalue is writable or not
 */
public class ClosureFactoryBuilder {
	private ClosureFactoryBuilder() {
	}

	public static final String FACTORY_NAME = "AbstractFactory$";
	public static final String CLOSURE_NAME = "ClosureFactory$";
	public static final String FACTORY_SUFFIX = "$Factory";
	public static final String BUILD_NAME = "build";

	/**
	 * Build an abstract class
	 *
	 * @param upvalues List of upvalues, marking if they are writable or not.
	 * @return The class writer, it has not been finalised yet.
	 */
	public static ClassWriter buildAbstractFactory(boolean... upvalues) {
		ClassWriter writer = new ClassWriter(0);

		String name = PREFIX + FACTORY_NAME + getModifierName(upvalues);
		writer.visit(V1_6, ACC_PUBLIC | ACC_ABSTRACT, name, null, "java/lang/Object", null);

		AsmUtils.writeDefaultConstructor(writer, "java/lang/Object");

		writer.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, BUILD_NAME, getSignature(upvalues), null, null).visitEnd();

		return writer;
	}

	/**
	 * Build an class that builds a {@link org.squiddev.luaj.luajc.function.CompilingClosure}.
	 *
	 * @param upvalues List of upvalues, marking if they are writable or not.
	 * @return The class writer, it has not been finalised yet.
	 */
	public static ClassWriter buildClosureFactory(boolean... upvalues) {
		String modifier = getModifierName(upvalues);
		String superName = PREFIX + FACTORY_NAME + modifier;
		String name = PREFIX + CLOSURE_NAME + modifier;

		ClassWriter writer = new ClassWriter(0);

		writer.visit(V1_6, ACC_PUBLIC | ACC_FINAL, name, null, superName, null);
		writer.visitField(ACC_PRIVATE | ACC_FINAL, "proto", TYPE_PROTOTYPE, null, null).visitEnd();

		{
			MethodVisitor constructor = writer.visitMethod(ACC_PUBLIC, "<init>", "(" + TYPE_PROTOTYPE + ")V", null, null);
			constructor.visitCode();
			AsmUtils.writeSuperConstructor(constructor, superName);

			constructor.visitVarInsn(ALOAD, 0);
			constructor.visitVarInsn(ALOAD, 1);
			constructor.visitFieldInsn(PUTFIELD, name, "proto", TYPE_PROTOTYPE);

			constructor.visitInsn(RETURN);
			constructor.visitMaxs(2, 2);
			constructor.visitEnd();
		}

		{
			MethodVisitor builder = writer.visitMethod(ACC_PUBLIC, BUILD_NAME, getSignature(upvalues), null, null);
			builder.visitCode();
			int closureIndex = upvalues.length + 2;

			builder.visitTypeInsn(NEW, CLASS_CLOSURE);

			builder.visitInsn(DUP);
			builder.visitVarInsn(ALOAD, 0);
			builder.visitFieldInsn(GETFIELD, name, "proto", TYPE_PROTOTYPE);
			builder.visitVarInsn(ALOAD, 1);
			builder.visitMethodInsn(INVOKESPECIAL, CLASS_CLOSURE, "<init>", "(" + TYPE_PROTOTYPE + TYPE_LUAVALUE + ")V", false);
			builder.visitVarInsn(ASTORE, closureIndex);

			builder.visitVarInsn(ALOAD, closureIndex);
			builder.visitFieldInsn(GETFIELD, CLASS_CLOSURE, "upvalues", "[" + TYPE_UPVALUE);
			builder.visitVarInsn(ASTORE, closureIndex + 1);

			for (int i = 0; i < upvalues.length; i++) {
				builder.visitVarInsn(ALOAD, closureIndex + 1);
				AsmUtils.constantOpcode(builder, i);
				builder.visitVarInsn(ALOAD, i + 2);

				if (!upvalues[i]) METHOD_NEW_UPVALUE_VALUE.inject(builder);
				builder.visitInsn(AASTORE);
			}

			builder.visitVarInsn(ALOAD, closureIndex);
			builder.visitInsn(ARETURN);
			builder.visitMaxs(4, upvalues.length + 4); // Upvalues + Env + This + Closure + Upvalue array
		}

		return writer;
	}

	/**
	 * Build a factory that loads a specific compiled class
	 *
	 * @param className The name of the compiled class;
	 * @param upvalues  List of upvalues, marking if they are writable or not.
	 * @return The class writer, it has not been finalised yet.
	 */
	public static ClassWriter buildCompiledFactory(String className, boolean... upvalues) {
		String modifier = getModifierName(upvalues);
		String superName = PREFIX + FACTORY_NAME + modifier;
		String name = className + FACTORY_SUFFIX;

		ClassWriter writer = new ClassWriter(0);

		writer.visit(V1_6, ACC_PUBLIC | ACC_FINAL, name, null, superName, null);

		AsmUtils.writeDefaultConstructor(writer, superName);

		{
			MethodVisitor builder = writer.visitMethod(ACC_PUBLIC, BUILD_NAME, getSignature(upvalues), null, null);
			builder.visitCode();
			int closureIndex = upvalues.length + 2;

			builder.visitTypeInsn(NEW, className);

			builder.visitInsn(DUP);
			builder.visitVarInsn(ALOAD, 1);
			builder.visitMethodInsn(INVOKESPECIAL, CLASS_CLOSURE, "<init>", "(" + TYPE_LUAVALUE + ")V", false);
			builder.visitVarInsn(ASTORE, closureIndex);

			for (int i = 0; i < upvalues.length; i++) {
				builder.visitVarInsn(ALOAD, closureIndex);
				builder.visitVarInsn(ALOAD, closureIndex + 1);
				builder.visitFieldInsn(PUTFIELD, className, PREFIX_UPVALUE + i, upvalues[i] ? TYPE_UPVALUE : TYPE_LUAVALUE);
			}

			builder.visitVarInsn(ALOAD, closureIndex);
			builder.visitInsn(ARETURN);
			builder.visitMaxs(3, upvalues.length + 3); // Upvalues + Env + This + Closure
		}

		return writer;
	}

	public static String getModifierName(boolean... upvalues) {
		StringBuilder name = new StringBuilder(upvalues.length);
		for (boolean writable : upvalues) {
			name.append(writable ? "W" : "R");
		}

		return name.toString();
	}

	public static String getSignature(boolean... upvalues) {
		StringBuilder signature = new StringBuilder("(").append(TYPE_LUAVALUE);
		for (boolean writable : upvalues) {
			signature.append(writable ? TYPE_UPVALUE : TYPE_LUAVALUE);
		}
		signature.append(")").append(TYPE_CLOSURE);

		return signature.toString();
	}
}
