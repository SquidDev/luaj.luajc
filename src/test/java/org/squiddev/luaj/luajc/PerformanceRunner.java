package org.squiddev.luaj.luajc;

import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.luajc.luaj.luajc.LuaJC;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * Main file to test performance of compilers
 */
public class PerformanceRunner {
	public static boolean QUIET = true;

	public static void main(String[] args) throws Exception {
		int times = 5;
		boolean luaC = true;
		boolean luaJC = true;

		// Ugly parse arguments
		if (args.length > 0) {
			Queue<String> arg = new ArrayDeque<>(Arrays.asList(args));
			String next;
			while ((next = arg.poll()) != null) {
				if (next.startsWith("--")) {
					next = next.substring(2);
				} else if (next.startsWith("-")) {
					next = next.substring(1);
				}
				switch (next) {
					case "t":
					case "times":
						String number = arg.poll();
						if (number == null) throw new IllegalArgumentException();
						times = Integer.parseInt(number);
						break;
					case "j":
					case "luajc":
						luaJC = false;
						break;
					case "l":
					case "luac":
						luaC = false;
						break;
					case "v":
					case "verbose":
						QUIET = false;
						break;
					case "q":
					case "quiet":
						QUIET = true;
						break;
					case "p":
					case "prompt":
						System.in.read();
						break;
					default:
						System.out.print(
							"Args\n" +
								"  -t|--times <number> Run this n times\n" +
								"  -j|--luajc          Don't run LuaJC\n" +
								"  -l|--luac           Don't run LuaC\n" +
								"  -v|--verbose        Verbose output\n" +
								"  -q|--quiet          Quiet output\n" +
								"  -p|--prompt         Prompt to begin\n"
						);
						return;
				}
			}
		}

		for (int i = 0; i < times; i++) {
			System.out.println("Iteration " + (i + 1) + "/" + times);

			if (luaC) testLuaC();
			if (luaJC) testLuaJC();
		}
	}

	public static void testLuaC() {
		LuaTable globals = getGlobals();
		LuaC.install();

		System.out.print("LuaC" + (QUIET ? "\t" : "\n"));
		execute(globals);
	}

	public static void testLuaJC() {
		LuaTable globals = getGlobals();
		LuaJC.install();

		System.out.print("LuaJC" + (QUIET ? "\t" : "\n"));
		execute(globals);
	}

	protected static LuaTable getGlobals() {
		LuaTable globals = JsePlatform.debugGlobals();

		return globals;
	}

	protected static void execute(LuaTable globals) {
		if (QUIET) installQuite(globals);

		try {
			InputStream aesStream = PerformanceRunner.class.getResourceAsStream("/org/squiddev/luaj/luajc/aes/AesLua.lua");
			InputStream speedStream = PerformanceRunner.class.getResourceAsStream("/org/squiddev/luaj/luajc/aes/AesSpeed.lua");

			long start = System.nanoTime();
			LuaFunction aes = LoadState.load(aesStream, "AesLua.lua", globals);
			LuaFunction speed = LoadState.load(speedStream, "AesSpeed.lua", globals);

			long compiled = System.nanoTime();

			aes.invoke();
			for (int i = 0; i < 10; i++) {
				speed.invoke();
			}

			long finished = System.nanoTime();

			System.out.printf("\n\tCompilation: %1$f\n\tRunning: %2$f\n", (compiled - start) / 1e9, (finished - compiled) / 1e9);
			System.out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static void installQuite(LuaTable globals) {
		globals.set("print", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				System.out.print("#");
				return LuaValue.NONE;
			}
		});
	}
}
