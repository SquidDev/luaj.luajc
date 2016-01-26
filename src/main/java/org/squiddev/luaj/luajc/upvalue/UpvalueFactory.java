package org.squiddev.luaj.luajc.upvalue;

import org.luaj.vm2.LuaValue;

/**
 * A static factory class for creating upvalues
 */
public final class UpvalueFactory {
	private UpvalueFactory() {
	}

	/**
	 * Create an empty upvalue
	 *
	 * @return Empty upvalue
	 */
	public static ReferenceUpvalue emptyUpvalue() {
		return new ReferenceUpvalue();
	}

	/**
	 * Create an upvalue set to {@link LuaValue#NIL}
	 *
	 * @return Upvalue with value set to {@link LuaValue#NIL}
	 */
	public static ReferenceUpvalue nilUpvalue() {
		return new ReferenceUpvalue(LuaValue.NIL);
	}

	/**
	 * Create an upvalue with a value
	 *
	 * @param v The value to set it to
	 * @return Upvalue with value set to {@code v}
	 */
	public static ReferenceUpvalue valueUpvalue(LuaValue v) {
		return new ReferenceUpvalue(v);
	}

	/**
	 * Create a new proxy upvalue
	 *
	 * @param upvalue The upvalue to proxy
	 * @return The proxied upvalue
	 */
	public static ProxyUpvalue proxy(AbstractUpvalue upvalue) {
		return new ProxyUpvalue(upvalue);
	}
}
