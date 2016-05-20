package org.squiddev.cobalt.luajc.function.executors;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.luajc.function.FunctionExecutor;
import org.squiddev.cobalt.luajc.function.FunctionWrapper;

/**
 * Function executor with 1 arguments
 */
public abstract class ArgExecutor2 extends FunctionExecutor {
	@Override
	public final Varargs execute(LuaState state, FunctionWrapper function, Varargs varargs) {
		return execute(state, function, varargs.first(), varargs.arg(2));
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function) {
		return execute(state, function, Constants.NIL, Constants.NIL);
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1) {
		return execute(state, function, arg1, Constants.NIL);
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return execute(state, function, arg1, arg2);
	}
}
