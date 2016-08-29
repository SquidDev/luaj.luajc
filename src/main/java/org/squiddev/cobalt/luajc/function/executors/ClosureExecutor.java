package org.squiddev.cobalt.luajc.function.executors;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.luajc.analysis.ProtoInfo;
import org.squiddev.cobalt.luajc.function.FunctionExecutor;
import org.squiddev.cobalt.luajc.function.FunctionWrapper;
import org.squiddev.cobalt.luajc.function.LuaVM;

import static org.squiddev.cobalt.Constants.NILS;
import static org.squiddev.cobalt.Constants.NONE;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Rewrite of {@link org.squiddev.cobalt.function.LuaInterpreter} which will compile when
 * a the function has been called {@link ProtoInfo#threshold} number of times.
 */
public final class ClosureExecutor extends FunctionExecutor {
	public static final ClosureExecutor INSTANCE = new ClosureExecutor();

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function) {
		ProtoInfo info = function.info;
		if (++info.calledClosure >= info.threshold) {
			FunctionExecutor executor = info.loader.include(info);
			return executor.execute(state, function);
		}

		int size = function.prototype.maxstacksize;

		LuaValue[] stack = new LuaValue[size];
		System.arraycopy(NILS, 0, stack, 0, size);

		return LuaVM.execute(state, function, stack, NONE, 0).eval(state).first();
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg) {
		ProtoInfo info = function.info;
		if (++info.calledClosure >= info.threshold) {
			FunctionExecutor executor = info.loader.include(info);
			return executor.execute(state, function, arg);
		}

		Prototype prototype = function.prototype;
		int size = prototype.maxstacksize;

		LuaValue[] stack = new LuaValue[size];
		System.arraycopy(NILS, 0, stack, 0, size);

		switch (prototype.numparams) {
			default:
				stack[0] = arg;
				return LuaVM.execute(state, function, stack, NONE, 0).eval(state).first();
			case 0:
				return LuaVM.execute(state, function, stack, arg, 0).eval(state).first();
		}
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2) {
		ProtoInfo info = function.info;
		if (++info.calledClosure >= info.threshold) {
			FunctionExecutor executor = info.loader.include(info);
			return executor.execute(state, function, arg1, arg2);
		}

		Prototype prototype = function.prototype;
		int size = prototype.maxstacksize;

		LuaValue[] stack = new LuaValue[size];
		System.arraycopy(NILS, 0, stack, 0, size);

		switch (prototype.numparams) {
			default:
				stack[0] = arg1;
				stack[1] = arg2;
				return LuaVM.execute(state, function, stack, NONE, 0).eval(state).first();
			case 1:
				stack[0] = arg1;
				return LuaVM.execute(state, function, stack, arg2, 0).eval(state).first();
			case 0:
				return LuaVM.execute(state, function, stack, prototype.is_vararg != 0 ? varargsOf(arg1, arg2) : NONE, 0).eval(state).first();
		}
	}

	@Override
	public final LuaValue execute(LuaState state, FunctionWrapper function, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		ProtoInfo info = function.info;
		if (++info.calledClosure >= info.threshold) {
			FunctionExecutor executor = info.loader.include(info);
			return executor.execute(state, function, arg1, arg2, arg3);
		}

		Prototype prototype = function.prototype;
		int size = prototype.maxstacksize;

		LuaValue[] stack = new LuaValue[size];
		System.arraycopy(NILS, 0, stack, 0, size);

		switch (prototype.numparams) {
			default:
				stack[0] = arg1;
				stack[1] = arg2;
				stack[2] = arg3;
				return LuaVM.execute(state, function, stack, NONE, 0).eval(state).first();
			case 2:
				stack[0] = arg1;
				stack[1] = arg2;
				return LuaVM.execute(state, function, stack, arg3, 0).eval(state).first();
			case 1:
				stack[0] = arg1;
				return LuaVM.execute(state, function, stack, prototype.is_vararg != 0 ? varargsOf(arg2, arg3) : NONE, 0).eval(state).first();
			case 0:
				return LuaVM.execute(state, function, stack, prototype.is_vararg != 0 ? varargsOf(arg1, arg2, arg3) : NONE, 0).eval(state).first();
		}
	}

	@Override
	public final Varargs execute(LuaState state, FunctionWrapper function, Varargs varargs) {
		ProtoInfo info = function.info;
		if (++info.calledClosure >= info.threshold) {
			FunctionExecutor executor = info.loader.include(info);
			return executor.execute(state, function, varargs);
		}

		Prototype prototype = function.prototype;
		int size = prototype.maxstacksize;

		LuaValue[] stack = new LuaValue[size];
		System.arraycopy(NILS, 0, stack, 0, size);

		int numParams = prototype.numparams;
		for (int i = 0; i < numParams; i++) {
			stack[i] = varargs.arg(i + 1);
		}
		return LuaVM.execute(state, function, stack, prototype.is_vararg != 0 ? varargs.subargs(numParams + 1) : NONE, 0);
	}
}
