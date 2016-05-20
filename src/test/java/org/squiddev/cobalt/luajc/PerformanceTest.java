package org.squiddev.cobalt.luajc;

import org.junit.Ignore;
import org.junit.Test;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.compiler.LuaC;

/**
 * Test the performance between LuaC and LuaJC
 */
@Ignore
public class PerformanceTest {
	@Test
	public void testLuaC() {
		LuaState state = LuaEnv.makeState();
		LuaTable globals = PerformanceRunner.getGlobals(state);
		LuaC.install(state);
		PerformanceRunner.testRun("LuaC", state, globals, false);
	}

	@Test
	public void testLuaJC() {
		LuaState state = LuaEnv.makeState();
		LuaTable globals = PerformanceRunner.getGlobals(state);
		LuaJC.install(state);
		PerformanceRunner.testRun("LuaJC", state, globals, false);
	}
}
