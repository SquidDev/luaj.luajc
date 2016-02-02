package org.squiddev.luaj.luajc;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.util.Collection;

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
	protected LuaValue globals;

	public PerformanceCompile(String name) {
		this.name = name;
	}

	@Before
	public void setup() {
		globals = JsePlatform.debugGlobals();
	}

	@Test
	public void compileLuaJC() throws Exception {
		LuaJC.install(new CompileOptions(CompileOptions.PREFIX, 0, CompileOptions.TYPE_THRESHOLD, true));
		load();
	}

	@Test
	public void compileLuaC() throws Exception {
		LuaC.install();
		load();
	}

	public void load() throws IOException {
		for (int i = 0; i < 10; i++) {
			LoadState.load(getClass().getResourceAsStream("/org/squiddev/luaj/luajc/compileonly/" + name + ".lua"), name + ".lua", globals);
		}
	}
}
