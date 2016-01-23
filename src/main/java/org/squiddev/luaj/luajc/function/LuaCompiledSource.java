package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.squiddev.luaj.luajc.IGetSource;

/**
 * A wrapper for functions to handle call stacks
 */
public final class LuaCompiledSource extends LuaFunction implements IGetSource {
	public int line;
	protected final LuaCompiledFunction parent;

	public LuaCompiledSource(LuaCompiledFunction parent) {
		line = parent.getCurrentLine();
		this.parent = parent;
	}

	@Override
	public int getCurrentLine() {
		return line;
	}

	@Override
	public Prototype getPrototype() {
		return parent.getPrototype();
	}

	@Override
	public LuaValue getfenv() {
		return parent.getfenv();
	}

	@Override
	public void setfenv(LuaValue env) {
		parent.setfenv(env);
	}

	@Override
	public boolean isclosure() {
		return true;
	}
}
