package org.squiddev.cobalt.luajc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.Arrays;
import java.util.Collection;

import static org.squiddev.cobalt.LuaString.valueOf;

/**
 * A series of large programs, just to test everything works.
 */
@RunWith(Parameterized.class)
public class CompileOnlyTest {
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getLua() {
		return Arrays.asList(new Object[][]{
			{"vBenchmark"},
			{"Howl"},
			{"luaide"},
		});
	}

	protected final String name;
	protected LuaTable globals;
	protected LuaState state;

	public CompileOnlyTest(String name) {
		this.name = name;
	}

	@Before
	public void setup() {
		state = LuaEnv.makeState();
		globals = LuaEnv.makeGlobals(state);
	}

	@Test
	public void testLuaJC() throws Exception {
		Loader.install(state, 0);
		state.compiler.load(Loader.load("compileonly/" + name), valueOf("@" + name + ".lua"), globals);
	}
}
