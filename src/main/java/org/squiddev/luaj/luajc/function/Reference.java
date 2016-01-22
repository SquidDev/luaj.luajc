package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.LuaValue;

/**
 * Reference to a LuaValue
 */
public final class Reference {
	public LuaValue value;

	public Reference() {
	}

	public Reference(LuaValue value) {
		this.value = value;
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, leave it empty
	 * @return Empty upvalue
	 */
	public static Reference newupe() {
		return new Reference();
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, initialize with nil
	 * @return Upvalue with value set to NIL
	 */
	public static Reference newupn() {
		return new Reference(LuaValue.NIL);
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, initialize with value
	 * @param v The value to set it to
	 * @return Upvalue with value set to {@code v}
	 */
	public static Reference newupl(LuaValue v) {
		return new Reference(v);
	}
}
