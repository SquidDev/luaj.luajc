package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.squiddev.luaj.luajc.IGetSource;

/**
 * A wrapper for functions to handle call stacks
 */
public class LuaCompiledSource extends LuaFunction implements IGetSource {
	public int line;
	protected final LuaCompiledFunction parent;

	public LuaCompiledSource(LuaCompiledFunction parent) {
		line = parent.getLine();
		this.parent = parent;
	}

	@Override
	public String getSource() {
		return parent.getSource();
	}

	@Override
	public int getLine() {
		return line;
	}

	@Override
	public Prototype getPrototype() {
		return parent.getPrototype();
	}

	public LuaValue getfenv() {
		return parent.getfenv();
	}

	public void setfenv(LuaValue env) {
		parent.setfenv(env);
	}
}
