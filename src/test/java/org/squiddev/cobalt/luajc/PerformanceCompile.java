package org.squiddev.cobalt.luajc;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.compiler.LuaC;

import java.io.IOException;
import java.util.Collection;

import static org.squiddev.cobalt.LuaString.valueOf;

/**
 * Used to test the performance of compilation
 */
@RunWith(Parameterized.class)
@Ignore
public class PerformanceCompile {
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getLua() {
		return CompileOnlyTest.getLua();
	}

	protected final String name;
	protected LuaTable globals;
	private LuaState state;

	public PerformanceCompile(String name) {
		this.name = name;
	}

	@Before
	public void setup() {
		state = LuaEnv.makeState();
		globals = LuaEnv.makeGlobals(state);
	}

	@Test
	public void compileLuaJC() throws Exception {
		Loader.install(state, 0);
		load();
	}

	@Test
	public void compileLuaC() throws Exception {
		LuaC.install(state);
		load();
	}

	public void load() throws IOException {
		for (int i = 0; i < 10; i++) {
			state.compiler.load(Loader.load("compileonly/" + name), valueOf(name + ".lua"), globals);
		}
	}
}
