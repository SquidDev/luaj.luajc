package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.LuaValue;

/**
 * Reference to a LuaValue
 */
public class Reference {
	public LuaValue value;

	public Reference() {
	}

	public Reference(LuaValue value) {
		this.value = value;
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, leave it empty
	 */
	public static Reference newupe() {
		return new Reference();
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, initialize with nil
	 */
	public static Reference newupn() {
		return new Reference(LuaValue.NIL);
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, initialize with value
	 */
	public static Reference newupl(LuaValue v) {
		return new Reference(v);
	}
}
