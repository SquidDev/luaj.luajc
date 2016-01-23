package org.squiddev.luaj.luajc.upvalue;

import org.luaj.vm2.LuaValue;

/**
 * An upvalue backed by an array and an index.
 */
public final class ArrayUpvalue extends AbstractUpvalue {
	private final LuaValue[] values;
	private final int slot;

	public ArrayUpvalue(LuaValue[] values, int slot) {
		this.values = values;
		this.slot = slot;
	}

	@Override
	public void setUpvalue(LuaValue value) {
		values[slot] = value;
	}

	@Override
	public LuaValue getUpvalue() {
		return values[slot];
	}
}
