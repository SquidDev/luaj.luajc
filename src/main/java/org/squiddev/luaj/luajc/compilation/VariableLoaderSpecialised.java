package org.squiddev.luaj.luajc.compilation;

import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.analysis.type.BasicType;
import org.squiddev.luaj.luajc.analysis.type.TypeInfo;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.luajc.Constants.METHOD_GET_UPVALUE;
import static org.squiddev.luaj.luajc.compilation.BuilderHelpers.getLoadOpcode;
import static org.squiddev.luaj.luajc.compilation.BuilderHelpers.getStoreOpcode;

/**
 * A variable loader that loads specialised variables
 */
public final class VariableLoaderSpecialised extends VariableLoader {
	public VariableLoaderSpecialised(JavaBuilder builder, MethodVisitor main, ProtoInfo info) {
		super(info, main, builder);
	}

	@Override
	public void loadLocal(VarInfo info, boolean specialist) {
		int slot = info.slot;
		boolean isUpvalue = info.isUpvalueRefer();
		if (isUpvalue) {
			assert !specialist : "Cannot load non-value upvalue";

			main.visitVarInsn(ALOAD, builder.findUpvalueIndex(slot));
			METHOD_GET_UPVALUE.inject(main);
		} else if (specialist) {
			assert info.type != BasicType.VALUE : "Value is not a specialist type";
			main.visitVarInsn(getLoadOpcode(info.type), builder.findTypedSlot(slot, info.type));
		} else if (info.type == BasicType.VALUE || info.getTypeInfo().valueReferenced) {
			main.visitVarInsn(ALOAD, builder.findTypedSlot(slot, BasicType.VALUE));
		} else {
			throw new IllegalStateException("Value of var " + info + " is never referenced");
		}
	}

	@Override
	public void storeLocal(VarInfo var, BasicType fromType) {
		boolean isUpvalue = var.isUpvalueAssign();
		if (isUpvalue) {
			if (fromType != BasicType.VALUE) specialToValue(fromType);
			storeLocalUpvalue(var);
		} else if (fromType != BasicType.VALUE) {
			TypeInfo info = var.getTypeInfo();

			if (var.type == BasicType.VALUE) {
				specialToValue(fromType);
				main.visitVarInsn(ASTORE, builder.findTypedSlot(var.slot, BasicType.VALUE));
			} else {
				assert fromType == var.type : "Cannot cross convert types";

				if (info.valueReferenced) {
					builder.dup(fromType);
					specialToValue(fromType);
					main.visitVarInsn(ASTORE, builder.findTypedSlot(var.slot, BasicType.VALUE));
				}

				main.visitVarInsn(getStoreOpcode(var.type), builder.findTypedSlot(var.slot, var.type));
			}
		} else {
			TypeInfo info = var.getTypeInfo();
			if (info.specialisedReferenced) main.visitInsn(DUP);

			// Always store the value. Just in case
			main.visitVarInsn(ASTORE, builder.findTypedSlot(var.slot, BasicType.VALUE));

			if (info.specialisedReferenced) {
				valueToSpecial(var.type, var.pc);
				main.visitVarInsn(getStoreOpcode(var.type), builder.findTypedSlot(var.slot, var.type));
			}
		}
	}

	@Override
	public void refreshLocal(VarInfo var) {
		boolean isUpvalue = var.isUpvalueAssign();
		if (isUpvalue) {
			main.visitVarInsn(ALOAD, builder.findTypedSlot(var.slot, BasicType.VALUE));
			storeLocalUpvalue(var);
		} else {
			// We only need to load if we used a specialist value
			if (var.getTypeInfo().specialisedReferenced) {
				main.visitVarInsn(ALOAD, builder.findTypedSlot(var.slot, BasicType.VALUE));
				valueToSpecial(var.type, var.pc);
				main.visitVarInsn(getStoreOpcode(var.type), builder.findTypedSlot(var.slot, var.type));
			}
		}
	}
}
