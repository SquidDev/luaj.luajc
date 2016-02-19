package org.squiddev.luaj.luajc;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

/**
 * Helpers for a Lua test environment
 */
public class LuaEnv {
	public static LuaTable makeGlobals() {
		return makeGlobals(false);
	}

	public static LuaTable makeGlobals(boolean stdout) {
		LuaTable globals = JsePlatform.debugGlobals();

		if (!stdout) {
			JseBaseLib lib = new JseBaseLib();
			lib.STDOUT = new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
				}
			});

			globals.load(lib);
		}
		globals.set("assertEquals", new AssertFunction());
		globals.set("assertMany", new AssertManyFunction());

		return globals;
	}

	public static class AssertFunction extends ThreeArgFunction {
		@Override
		public LuaValue call(LuaValue expected, LuaValue actual, LuaValue message) {
			String msg = message.toString();
			if (message.isnil()) {
				msg = "(No message)";
			}

			assertEquals(msg, expected.tojstring(), actual.tojstring());
			assertEquals(msg, expected.typename(), actual.typename());

			return LuaValue.NONE;
		}
	}

	public static class AssertManyFunction extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs args) {
			int nArgs = args.narg() / 2;
			for (int i = 1; i <= nArgs; i++) {
				LuaValue expected = args.arg(i);
				LuaValue actual = args.arg(i + nArgs);

				assertEquals("Type mismatch at arg #" + i, expected.typename(), actual.typename());
				assertEquals("Value mismatch at arg #" + i, expected.tojstring(), actual.tojstring());
			}

			return LuaValue.NONE;
		}
	}

}
