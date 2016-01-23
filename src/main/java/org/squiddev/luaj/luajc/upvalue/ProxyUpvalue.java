package org.squiddev.luaj.luajc.upvalue;

import org.luaj.vm2.LuaValue;

/**
 * A upvalue that delegates to another upvalue.
 */
public final class ProxyUpvalue extends AbstractUpvalue {
	public AbstractUpvalue upvalue;

	public ProxyUpvalue(AbstractUpvalue upvalue) {
		this.upvalue = upvalue;
	}

	@Override
	public void setUpvalue(LuaValue value) {
		upvalue.setUpvalue(value);
	}

	@Override
	public LuaValue getUpvalue() {
		return upvalue.getUpvalue();
	}

	public static ProxyUpvalue create(AbstractUpvalue upvalue) {
		return new ProxyUpvalue(upvalue);
	}
}
