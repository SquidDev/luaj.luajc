package org.squiddev.cobalt.luajc.function.executors;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.luajc.function.FunctionExecutor;
import org.squiddev.cobalt.luajc.function.FunctionWrapper;

/**
 * Function executor with many arguments
 */
public abstract class ArgExecutorMany extends FunctionExecutor {
	@Override
	public LuaValue execute(LuaState state, FunctionWrapper function) {
		return execute(state, function, (Varargs) Constants.NONE).eval(state).first();
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1) {
		return execute(state, function, (Varargs) arg1).eval(state).first();
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2) {
		return execute(state, function, ValueFactory.varargsOf(arg1, arg2)).eval(state).first();
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return execute(state, function, ValueFactory.varargsOf(arg1, arg2, arg3)).eval(state).first();
	}
}
