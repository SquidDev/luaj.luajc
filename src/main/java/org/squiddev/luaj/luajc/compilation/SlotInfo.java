package org.squiddev.luaj.luajc.compilation;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.squiddev.luaj.luajc.Constants.*;

/**
 * Tracks information in a slot
 */
public final class SlotInfo {
	public final int luaSlot;

	public int valueSlot = -1;

	public int upvalueSlot = -1;

	public SlotInfo(int luaSlot) {
		this.luaSlot = luaSlot;
	}

	public void injectSlot(MethodVisitor visitor, Label start, Label end) {
		if (valueSlot >= 0) {
			visitor.visitLocalVariable(PREFIX_LOCAL_SLOT + "_" + luaSlot, TYPE_LUAVALUE, null, start, end, valueSlot);
		}

		if (upvalueSlot >= 0) {
			visitor.visitLocalVariable(PREFIX_UPVALUE_SLOT + "_" + luaSlot, TYPE_UPVALUE, null, start, end, upvalueSlot);
		}
	}
}
