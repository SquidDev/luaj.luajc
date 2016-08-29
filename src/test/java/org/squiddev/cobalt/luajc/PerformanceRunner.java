package org.squiddev.cobalt.luajc;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.ZeroArgFunction;
import org.squiddev.cobalt.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static org.squiddev.cobalt.LuaString.valueOf;

/**
 * Main file to test performance of compilers
 */
public class PerformanceRunner {
	public static boolean QUIET = true;

	public static void main(String[] args) throws Exception {
		int times = 5;
		boolean luaC = true;
		boolean luaJC = true;
		boolean luaVM = true;
		boolean compileOnly = false;

		// Ugly parse arguments
		if (args.length > 0) {
			Queue<String> arg = new ArrayDeque<String>(Arrays.asList(args));
			String next;
			while ((next = arg.poll()) != null) {
				if (next.startsWith("--")) {
					next = next.substring(2);
				} else if (next.startsWith("-")) {
					next = next.substring(1);
				}
				if (next.equals("t") || next.equals("times")) {
					String number = arg.poll();
					if (number == null) throw new IllegalArgumentException();
					times = Integer.parseInt(number);
				} else if (next.equals("j") || next.equals("luajc")) {
					luaJC = false;
				} else if (next.equals("l") || next.equals("luac")) {
					luaC = false;
				} else if (next.equals("o") || next.equals("luavm")) {
					luaVM = false;
				} else if (next.equals("v") || next.equals("verbose")) {
					QUIET = false;
				} else if (next.equals("q") || next.equals("quiet")) {
					QUIET = true;
				} else if (next.equals("p") || next.equals("prompt")) {
					System.out.print("Waiting for key...");
					while (true) {
						int key = System.in.read();
						if (key == -1) throw new IOException("Hit EOF");
						if (key == '\n') break;
					}
				} else if (next.equals("c") || next.equals("compile-only")) {
					compileOnly = true;
				} else {
					System.out.print(
						"Args\n" +
							"  -t|--times <number> Run this n times\n" +
							"  -j|--luajc          Don't run LuaJC\n" +
							"  -l|--luac           Don't run LuaC\n" +
							"  -o|--luavm          Don't run optimised LuaClosure\n" +
							"  -v|--verbose        Verbose output\n" +
							"  -q|--quiet          Quiet output\n" +
							"  -p|--prompt         Prompt to begin\n" +
							"  -c|--compile-only   Only test compilation\n"
					);
					return;
				}
			}
		}

		LuaState state = LuaEnv.makeState();
		for (int i = 0; i < times; i++) {
			System.out.println("Iteration " + (i + 1) + "/" + times);

			if (luaC) {
				LuaTable globals = getGlobals(state);
				LuaC.install(state);
				testRun("LuaC", state, globals, compileOnly);
			}
			if (luaJC) {
				LuaTable globals = getGlobals(state);
				LuaJC.install(state);
				testRun("LuaJC", state, globals, compileOnly);
			}

			if (luaVM) {
				LuaTable globals = getGlobals(state);
				LuaJC.install(state, new CompileOptions(CompileOptions.PREFIX, Integer.MAX_VALUE, true, null));
				testRun("LuaVM", state, globals, compileOnly);
			}
		}
	}

	public static void testRun(String name, LuaState state, LuaTable globals, boolean compileOnly) {
		System.out.print(name + (QUIET ? "\t" : "\n"));
		execute(state, globals, compileOnly);
	}

	protected static LuaTable getGlobals(LuaState state) {
		return JsePlatform.debugGlobals(state);
	}

	protected static void execute(LuaState state, LuaTable globals, boolean compileOnly) {
		if (QUIET) installQuite(globals);

		try {
			InputStream aesStream = PerformanceRunner.class.getResourceAsStream("/org/squiddev/cobalt/luajc/aes/AesLua.lua");
			InputStream speedStream = PerformanceRunner.class.getResourceAsStream("/org/squiddev/cobalt/luajc/aes/AesSpeed.lua");

			long start = System.nanoTime();
			LuaFunction aes = state.compiler.load(aesStream, valueOf("@AesLua.lua"), globals);
			LuaFunction speed = state.compiler.load(speedStream, valueOf("@AesSpeed.lua"), globals);

			long compiled = System.nanoTime();

			if (!compileOnly) {
				aes.call(state);
				for (int i = 0; i < 10; i++) {
					speed.call(state);
				}
			}

			long finished = System.nanoTime();

			System.out.printf("\n\tCompilation: %1$f\n\tRunning: %2$f\n", (compiled - start) / 1e9, (finished - compiled) / 1e9);
			System.out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static void installQuite(LuaTable globals) {
		globals.rawset("print", new ZeroArgFunction() {
			@Override
			public LuaValue call(LuaState state) {
				System.out.print("#");
				return Constants.NONE;
			}
		});
	}
}
