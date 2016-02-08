package org.squiddev.luaj.luajc.compilation;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.squiddev.luaj.luajc.Constants.*;

/**
 * Storage for various slots
 */
public final class SlotInfo {
	public final int luaSlot;
	public int booleanSlot = -1;
	public int numberSlot = -1;
	public int valueSlot = -1;
	public int upvalueSlot = -1;

	public SlotInfo(int luaSlot) {
		this.luaSlot = luaSlot;
	}

	public void visitLocals(MethodVisitor visitor, Label start, Label end) {
		if (valueSlot >= 0) {
			visitor.visitLocalVariable(PREFIX_LOCAL + "_" + luaSlot, TYPE_LUAVALUE, null, start, end, valueSlot);
		}
		if (booleanSlot >= 0) {
			visitor.visitLocalVariable(PREFIX_LOCAL_BOOLEAN + "_" + luaSlot, "Z", null, start, end, booleanSlot);
		}
		if (numberSlot >= 0) {
			visitor.visitLocalVariable(PREFIX_LOCAL_NUMBER + "_" + luaSlot, "D", null, start, end, numberSlot);
		}
		if (upvalueSlot >= 0) {
			visitor.visitLocalVariable(PREFIX_LOCAL_UPVALUE + "_" + luaSlot, TYPE_UPVALUE, null, start, end, upvalueSlot);
		}
	}

	@Override
	public String toString() {
		return "{" + "u=" + upvalueSlot + ", v=" + valueSlot + ", n=" + numberSlot + ", b=" + booleanSlot + '}';
	}
}
