package org.squiddev.luaj.luajc.upvalue;

import org.luaj.vm2.LuaValue;

/**
 * An upvalue that is backed by a variable.
 */
public final class ReferenceUpvalue extends AbstractUpvalue {
	private LuaValue value;

	public ReferenceUpvalue() {
	}

	public ReferenceUpvalue(LuaValue value) {
		this.value = value;
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, leave it empty
	 *
	 * @return Empty upvalue
	 */
	public static ReferenceUpvalue newupe() {
		return new ReferenceUpvalue();
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, initialize with nil
	 *
	 * @return Upvalue with value set to NIL
	 */
	public static ReferenceUpvalue newupn() {
		return new ReferenceUpvalue(LuaValue.NIL);
	}

	/**
	 * Java code generation utility to allocate storage for upvalue, initialize with value
	 *
	 * @param v The value to set it to
	 * @return Upvalue with value set to {@code v}
	 */
	public static ReferenceUpvalue newupl(LuaValue v) {
		return new ReferenceUpvalue(v);
	}

	@Override
	public void setUpvalue(LuaValue value) {
		this.value = value;
	}

	@Override
	public LuaValue getUpvalue() {
		return value;
	}
}
