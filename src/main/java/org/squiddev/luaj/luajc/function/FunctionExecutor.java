package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

/**
 * An executor for functions. The caller must choose which method to
 * call based on the number of arguments
 */
public abstract class FunctionExecutor {
	public abstract Varargs execute(FunctionWrapper function, Varargs varargs);

	public abstract LuaValue execute(FunctionWrapper function);

	public abstract LuaValue execute(FunctionWrapper function, LuaValue arg1);

	public abstract LuaValue execute(FunctionWrapper function, LuaValue arg1, LuaValue arg2);

	public abstract LuaValue execute(FunctionWrapper function, LuaValue arg1, LuaValue arg2, LuaValue arg3);
}
