package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

/**
 * An implementation of {@link org.luaj.vm2.LuaFunction} where all
 * methods are final
 */
public abstract class BasicLuaFunction extends LuaFunction {
	public BasicLuaFunction(LuaValue env) {
		super(env);
	}

	@Override
	public final int type() {
		return TFUNCTION;
	}

	@Override
	public final String typename() {
		return "function";
	}

	@Override
	public final boolean isfunction() {
		return true;
	}

	@Override
	public final LuaValue checkfunction() {
		return this;
	}

	@Override
	public final LuaFunction optfunction(LuaFunction defval) {
		return this;
	}

	@Override
	public final LuaValue getmetatable() {
		return s_metatable;
	}

	@Override
	public final LuaValue getfenv() {
		return env;
	}

	@Override
	public final void setfenv(LuaValue env) {
		this.env = env != null ? env : NIL;
	}

	@Override
	public final boolean isclosure() {
		return true;
	}
}
