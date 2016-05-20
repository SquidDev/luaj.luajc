package org.squiddev.cobalt.luajc.upvalue;

import org.squiddev.cobalt.LuaValue;

/**
 * An upvalue may be a simple reference, an element in an array or
 * converted between the two, hence the need for an abstract class.
 *
 * @see ReferenceUpvalue
 */
public abstract class AbstractUpvalue {
	/**
	 * Set the contents of this upvalue
	 *
	 * @param value The value to set to
	 */
	public abstract void setUpvalue(LuaValue value);

	/**
	 * Set the contents of this upvalue
	 *
	 * @return The upvalue to set
	 */
	public abstract LuaValue getUpvalue();

	/**
	 * Close this upvalue.
	 * This will rewrite the proxy upvalue to point
	 * to a reference rather than an array.
	 */
	public void close() {
	}
}
