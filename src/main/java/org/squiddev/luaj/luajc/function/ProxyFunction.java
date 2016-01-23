package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;
import org.squiddev.luaj.luajc.IGetSource;

/**
 * An implementation of a LuaFunction that simply delegates to another function
 */
public final class ProxyFunction extends LuaFunction implements IGetSource {
	public LuaCompiledFunction function;

	public ProxyFunction(LuaCompiledFunction function) {
		this.function = function;
	}

	@Override
	public LuaValue call() {
		return function.call();
	}

	@Override
	public LuaValue call(LuaValue arg) {
		return function.call(arg);
	}

	@Override
	public LuaValue call(LuaValue arg1, LuaValue arg2) {
		return function.call(arg1, arg2);
	}

	@Override
	public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return function.call(arg1, arg2, arg3);
	}

	@Override
	public Varargs invoke(Varargs args) {
		return function.invoke(args);
	}

	public static ProxyFunction create(LuaCompiledFunction function) {
		return new ProxyFunction(function);
	}

	@Override
	public LuaValue getfenv() {
		return function.getfenv();
	}

	@Override
	public void setfenv(LuaValue env) {
		function.setfenv(env);
	}

	@Override
	public int getCurrentLine() {
		return function.getCurrentLine();
	}

	@Override
	public Prototype getPrototype() {
		return function.getPrototype();
	}
}
