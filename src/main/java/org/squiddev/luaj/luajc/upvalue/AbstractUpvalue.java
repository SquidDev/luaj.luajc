package org.squiddev.luaj.luajc.upvalue;

import org.luaj.vm2.LuaValue;

/**
 * An upvalue may be a simple reference, an element in an array or
 * converted between the two, hence the need for an abstract class.
 *
 * @see ReferenceUpvalue
 */
public abstract class AbstractUpvalue {
	public abstract void setUpvalue(LuaValue value);

	public abstract LuaValue getUpvalue();
}
