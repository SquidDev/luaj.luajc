package org.squiddev.luaj.luajc.compilation;

import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.analysis.type.BasicType;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.squiddev.luaj.luajc.Constants.METHOD_GET_UPVALUE;

/**
 * A variable loader that loads generic variables.
 */
public final class VariableLoaderGeneric extends VariableLoader {
	public VariableLoaderGeneric(JavaBuilder builder, MethodVisitor main, ProtoInfo info) {
		super(info, main, builder);
	}

	public final void loadLocal(int pc, int slot) {
		loadLocal(pc, slot, false);
	}

	public final void storeLocal(VarInfo var) {
		storeLocal(var, BasicType.VALUE);
	}

	public final void storeLocalNoChecks(VarInfo var) {
		storeLocalNoChecks(var, false);
	}

	@Override
	public void loadLocal(VarInfo info, boolean specialist) {
		int slot = info.slot;
		boolean isUpvalue = info.isUpvalueRefer();
		if (isUpvalue) {
			assert !specialist : "Cannot load non-value upvalue";

			main.visitVarInsn(ALOAD, builder.findUpvalueIndex(slot));
			METHOD_GET_UPVALUE.inject(main);
		} else {
			main.visitVarInsn(ALOAD, builder.findTypedSlot(slot, BasicType.VALUE));
		}
	}

	@Override
	public void storeLocal(VarInfo var, BasicType fromType) {
		boolean isUpvalue = var.isUpvalueAssign();
		if (isUpvalue) {
			if (fromType != BasicType.VALUE) specialToValue(fromType);
			storeLocalUpvalue(var);
		} else if (fromType != BasicType.VALUE) {
			specialToValue(fromType);
			main.visitVarInsn(ASTORE, builder.findTypedSlot(var.slot, BasicType.VALUE));
		} else {
			main.visitVarInsn(ASTORE, builder.findTypedSlot(var.slot, BasicType.VALUE));
		}
	}

	@Override
	public void refreshLocal(VarInfo var) {
		boolean isUpvalue = var.isUpvalueAssign();
		if (isUpvalue) {
			main.visitVarInsn(ALOAD, builder.findTypedSlot(var.slot, BasicType.VALUE));
			storeLocalUpvalue(var);
		}
	}
}
