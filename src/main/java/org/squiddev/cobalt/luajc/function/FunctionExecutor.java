package org.squiddev.cobalt.luajc.function;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;

/**
 * An executor for functions. The caller must choose which method to
 * call based on the number of arguments
 */
public abstract class FunctionExecutor {
	public abstract Varargs execute(LuaState state, FunctionWrapper function, Varargs varargs);

	public abstract LuaValue execute(LuaState state, FunctionWrapper function);

	public abstract LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1);

	public abstract LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2);

	public abstract LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2, LuaValue arg3);
}
