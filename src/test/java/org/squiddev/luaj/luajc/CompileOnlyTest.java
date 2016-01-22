package org.squiddev.luaj.luajc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.Arrays;
import java.util.Collection;

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
		});
	}

	protected final String name;
	protected LuaValue globals;

	public CompileOnlyTest(String name) {
		this.name = name;
	}

	@Before
	public void setup() {
		globals = JsePlatform.debugGlobals();
	}

	@Test
	public void testLuaJC() throws Exception {
		LuaJC.install();

		LoadState.load(getClass().getResourceAsStream("/org/squiddev/luaj/luajc/compileonly/" + name + ".lua"), name + ".lua", globals);
	}
}
