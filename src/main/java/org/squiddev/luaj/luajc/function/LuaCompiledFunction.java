package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.squiddev.luaj.luajc.IGetPrototype;

/**
 * Subclass of {@link LuaFunction} common to LuaJC compiled functions.
 *
 * Since lua functions can be called with too few or too many arguments,
 * and there are overloaded {@link LuaValue#call()} functions with varying
 * number of arguments, a compiled function exposed needs to handle the
 * argument fixup when a function is called with a number of arguments
 * differs from that expected.
 *
 * To simplify the creation of library functions,
 * there are 5 direct subclasses to handle common cases based on number of
 * argument values and number of return return values.
 * <ul>
 * <li>{@link ZeroArgFunction}</li>
 * <li>{@link OneArgFunction}</li>
 * <li>{@link TwoArgFunction}</li>
 * <li>{@link ThreeArgFunction}</li>
 * <li>{@link VarArgFunction}</li>
 * </ul>
 */
public abstract class LuaCompiledFunction extends LuaFunction implements IGetPrototype {
	public LuaCompiledFunction(LuaValue env) {
		super(env);
	}

	@Override
	public boolean isclosure() {
		return true;
	}
}
