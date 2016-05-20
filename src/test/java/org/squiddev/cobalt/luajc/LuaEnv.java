package org.squiddev.cobalt.luajc;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.OneArgFunction;
import org.squiddev.cobalt.function.ThreeArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;
import org.squiddev.cobalt.lib.platform.FileResourceManipulator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

/**
 * Helpers for a Lua test environment
 */
public class LuaEnv {
	public static LuaState makeState() {
		return new LuaState(new FileResourceManipulator());
	}

	public static LuaTable makeGlobals(LuaState state) {
		return makeGlobals(state, false);
	}

	public static LuaTable makeGlobals(LuaState state, boolean stdout) {
		LuaTable globals = JsePlatform.debugGlobals(state);

		if (!stdout) {
			state.stdout = new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException {
				}
			});
		}
		globals.rawset("assertEquals", new AssertFunction());
		globals.rawset("assertMany", new AssertManyFunction());
		globals.rawset("makeError", new MakeErrorFunction());

		return globals;
	}

	public static class AssertFunction extends ThreeArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue expected, LuaValue actual, LuaValue message) {
			String msg = message.toString();
			if (message.isNil()) {
				msg = "(No message)";
			}

			assertEquals(msg, expected.typeName(), actual.typeName());
			assertEquals(msg, expected.toString(), actual.toString());

			return Constants.NONE;
		}
	}

	public static class AssertManyFunction extends VarArgFunction {
		@Override
		public Varargs invoke(LuaState state, Varargs args) {
			int nArgs = args.count() / 2;
			for (int i = 1; i <= nArgs; i++) {
				LuaValue expected = args.arg(i);
				LuaValue actual = args.arg(i + nArgs);

				assertEquals("Type mismatch at arg #" + i, expected.typeName(), actual.typeName());
				assertEquals("Value mismatch at arg #" + i, expected.toString(), actual.toString());
			}

			return Constants.NONE;
		}
	}

	public static class MakeErrorFunction extends OneArgFunction {
		@Override
		public LuaValue call(LuaState state, LuaValue arg) {
			throw new RuntimeException(arg.optString("An error happened"));
		}
	}

}
