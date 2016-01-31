package org.squiddev.luaj.luajc.analysis.type;

import org.luaj.vm2.LuaValue;

/**
 * A primitive type
 */
public enum BasicType {
	BOOLEAN,
	NUMBER,
	VALUE;

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
}
