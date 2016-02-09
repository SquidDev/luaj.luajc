package org.squiddev.luaj.luajc.compilation;

import org.luaj.vm2.LuaValue;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.analysis.type.BasicType;
import org.squiddev.luaj.luajc.utils.AsmUtils;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.luajc.Constants.*;
import static org.squiddev.luaj.luajc.compilation.BuilderHelpers.getStoreOpcode;

/**
 * org.squiddev.luaj.luajc.compilation (luaj.luajc
 */
public abstract class VariableLoader {
	protected final JavaBuilder builder;
	protected final MethodVisitor main;
	protected final ProtoInfo info;

	public VariableLoader(ProtoInfo info, MethodVisitor main, JavaBuilder builder) {
		this.info = info;
		this.main = main;
		this.builder = builder;
	}

	public void loadLocal(int pc, int slot, boolean specialist) {
		loadLocal(info.getVariable(pc, slot), specialist);
	}

	public abstract void loadLocal(VarInfo info, boolean specialist);

	public final void specialToValue(BasicType type) {
		switch (type) {
			case BOOLEAN:
				METHOD_BOOL_TO_VALUE.inject(main);
				break;
			case NUMBER:
				METHOD_NUMBER_TO_VALUE.inject(main);
				break;
			default:
				throw new IllegalStateException("Type " + type + " is not a specialist type");
		}
	}

	public final void assertType(int typeNo, int pc) {
		Label success = new Label();

		main.visitInsn(DUP);
		METHOD_TYPE.inject(main);
		AsmUtils.constantOpcode(main, typeNo);

		main.visitJumpInsn(IF_ICMPNE, success);
		main.visitInsn(POP);
		builder.visitResume(pc);

		main.visitLabel(success);
	}

	public final void valueToSpecial(BasicType type, int pc) {
		switch (type) {
			case BOOLEAN:
				assertType(LuaValue.TBOOLEAN, pc);
				METHOD_VALUE_TO_BOOL.inject(main);
				break;
			case NUMBER:
				assertType(LuaValue.TNUMBER, pc);
				METHOD_VALUE_TO_NUMBER.inject(main);
				break;
			default:
				throw new IllegalStateException("Type " + type + " is not a specialist type");
		}
	}

	public final void storeLocalNoChecks(int pc, int slot, boolean specialist) {
		storeLocalNoChecks(pc < 0 ? info.params[slot] : info.vars[pc][slot], specialist);
	}

	/**
	 * Store a {@link LuaValue} without checking it was typed or not
	 *
	 * @param var        The variable to store
	 * @param specialist Store in a specialist slot
	 */
	public final void storeLocalNoChecks(VarInfo var, boolean specialist) {
		boolean isUpvalue = var.isUpvalueAssign();
		if (isUpvalue) {
			storeLocalUpvalue(var);
		} else if (specialist) {
			main.visitVarInsn(getStoreOpcode(var.type), builder.findTypedSlot(var.slot, var.type));
		} else {
			main.visitVarInsn(ASTORE, builder.findTypedSlot(var.slot, BasicType.VALUE));
		}
	}


	public final void storeLocal(int pc, int slot, BasicType fromType) {
		storeLocal(pc < 0 ? info.params[slot] : info.vars[pc][slot], fromType);
	}

	public abstract void storeLocal(VarInfo var, BasicType fromType);

	/**
	 * Load a local variable and refresh its subtypes
	 *
	 * @param var The types to load
	 * @param pc  The pc to jump to on failure
	 */
	public abstract void refreshLocal(VarInfo var, int pc);

	/**
	 * Store an upvalue, creating it if needed
	 *
	 * @param var The upvalue to create
	 */
	public final void storeLocalUpvalue(VarInfo var) {
		int index = builder.findUpvalueIndex(var.slot);
		boolean isUpCreate = var.isUpvalueCreate(var.pc);
		if (isUpCreate) {
			// As we are creating it we need to write
			METHOD_NEW_UPVALUE_VALUE.inject(main);

			// We should only proxy when we need to switch back into interpreted mode
			// and this upvalue will be mutated again
			METHOD_NEW_UPVALUE_PROXY.inject(main);
			main.visitVarInsn(ASTORE, index);
		} else {
			// Then store the upvalue in the index
			main.visitVarInsn(ALOAD, index);
			main.visitInsn(SWAP);
			METHOD_SET_UPVALUE.inject(main);
		}
	}
}
