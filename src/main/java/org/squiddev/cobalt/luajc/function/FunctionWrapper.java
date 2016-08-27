package org.squiddev.cobalt.luajc.function;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.luajc.analysis.ProtoInfo;
import org.squiddev.cobalt.luajc.upvalue.AbstractUpvalue;

/**
 * Subclass of {@link LuaFunction} common to LuaJC compiled functions.
 */
public final class FunctionWrapper extends LuaClosure {
	public final ProtoInfo info;
	public final Prototype prototype;

	public final AbstractUpvalue[] upvalues;

	public FunctionWrapper(ProtoInfo info, LuaTable env) {
		super(env);
		this.info = info;
		this.prototype = info.prototype;
		this.upvalues = new AbstractUpvalue[prototype.nups];
	}

	@Override
	public Prototype getPrototype() {
		return info.prototype;
	}

	@Override
	public LuaValue getUpvalue(int i) {
		return upvalues[i].getUpvalue();
	}

	@Override
	public void setUpvalue(int i, LuaValue v) {
		upvalues[i].setUpvalue(v);
	}

	@Override
	public LuaValue call(LuaState state) {
		return info.executor.execute(state, this);
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg) {
		return info.executor.execute(state, this, arg);
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) {
		return info.executor.execute(state, this, arg1, arg2);
	}

	@Override
	public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return info.executor.execute(state, this, arg1, arg2, arg3);
	}

	@Override
	public Varargs invoke(LuaState state, Varargs args) {
		return onInvoke(state, args).eval(state);
	}

	@Override
	public Varargs onInvoke(LuaState state, Varargs args) {
		return info.executor.execute(state, this, args);
	}
}
