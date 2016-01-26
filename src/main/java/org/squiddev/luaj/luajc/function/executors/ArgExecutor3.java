package org.squiddev.luaj.luajc.function.executors;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.squiddev.luaj.luajc.function.FunctionExecutor;
import org.squiddev.luaj.luajc.function.FunctionWrapper;

/**
 * Function executor with 1 arguments
 */
public abstract class ArgExecutor3 extends FunctionExecutor {
	@Override
	public Varargs execute(FunctionWrapper function, Varargs varargs) {
		return execute(function);
	}

	@Override
	public final LuaValue execute(FunctionWrapper function, LuaValue arg1) {
		return execute(function);
	}

	@Override
	public final LuaValue execute(FunctionWrapper function, LuaValue arg1, LuaValue arg2) {
		return execute(function);
	}

	@Override
	public final LuaValue execute(FunctionWrapper function, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return execute(function);
	}
}
