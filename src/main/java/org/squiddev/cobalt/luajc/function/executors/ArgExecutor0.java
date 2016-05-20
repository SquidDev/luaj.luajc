package org.squiddev.cobalt.luajc.function.executors;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.luajc.function.FunctionExecutor;
import org.squiddev.cobalt.luajc.function.FunctionWrapper;

/**
 * Function executor with 0 arguments
 */
public abstract class ArgExecutor0 extends FunctionExecutor {
	@Override
	public final Varargs execute(LuaState state, FunctionWrapper function, Varargs varargs) {
		return execute(state, function);
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1) {
		return execute(state, function);
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2) {
		return execute(state, function);
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return execute(state, function);
	}
}
