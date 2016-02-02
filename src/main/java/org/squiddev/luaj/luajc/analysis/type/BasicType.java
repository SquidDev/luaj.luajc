package org.squiddev.luaj.luajc.analysis.type;

import org.luaj.vm2.LuaValue;

/**
 * A primitive type
 */
public enum BasicType {
	BOOLEAN(1),
	NUMBER(2),
	VALUE(4);

	private int flag;

	BasicType(int flag) {
		this.flag = flag;
	}

	public static BasicType fromValue(LuaValue value) {
		switch (value.type()) {
			case LuaValue.TBOOLEAN:
				return BOOLEAN;
			case LuaValue.TNUMBER:
				return NUMBER;
			default:
				return VALUE;
		}
	}

	public String format() {
		return name().substring(0, 1);
	}

	public int flag() {
		return flag;
	}
}
