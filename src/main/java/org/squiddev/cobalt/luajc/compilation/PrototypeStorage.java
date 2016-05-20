package org.squiddev.cobalt.luajc.compilation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.cobalt.luajc.analysis.ProtoInfo;
import org.squiddev.cobalt.luajc.utils.AsmUtils;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.cobalt.luajc.compilation.Constants.*;

/**
 * Creates a static storage instance for {@link org.squiddev.cobalt.luajc.analysis.ProtoInfo}
 */
public final class PrototypeStorage {
	public static ClassWriter createStorage(String prefix, ProtoInfo info) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		writer.visit(V1_6, ACC_PUBLIC | ACC_SUPER | ACC_FINAL, prefix + PROTOTYPE_STORAGE, null, "java/lang/Object", null);

		AsmUtils.writeDefaultConstructor(writer, "java/lang/Object");

		MethodVisitor setup = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "setup", "(" + TYPE_PROTOINFO + ")V", null, null);
		createStorage(writer, setup, prefix, info, 0);
		setup.visitInsn(RETURN);
		setup.visitMaxs(0, 0);
		setup.visitEnd();

		return writer;
	}

	private static void createStorage(ClassVisitor writer, MethodVisitor method, String prefix, ProtoInfo info, int slot) {
		String name = PROTOTYPE_NAME + info.name;
		writer.visitField(ACC_PUBLIC | ACC_STATIC, name, TYPE_PROTOINFO, null, null).visitEnd();

		method.visitVarInsn(ALOAD, slot);
		method.visitFieldInsn(PUTSTATIC, prefix + PROTOTYPE_STORAGE, PROTOTYPE_NAME + info.name, TYPE_PROTOINFO);

		if (info.subprotos != null) {
			int length = info.subprotos.length;
			for (int i = 0; i < length; i++) {
				method.visitVarInsn(ALOAD, slot);
				method.visitFieldInsn(GETFIELD, CLASS_PROTOINFO, "subprotos", "[" + TYPE_PROTOINFO);
				AsmUtils.constantOpcode(method, i);
				method.visitInsn(AALOAD);
				method.visitVarInsn(ASTORE, slot + 1);
				createStorage(writer, method, prefix, info.subprotos[i], slot + 1);
			}
		}
	}
}
