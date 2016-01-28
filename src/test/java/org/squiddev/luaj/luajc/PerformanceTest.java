package org.squiddev.luaj.luajc;

import org.junit.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.compiler.LuaC;

/**
 * Test the performance between LuaC and LuaJC
 */
public class PerformanceTest {
	@Test
	public void testLuaC() {
		LuaTable globals = PerformanceRunner.getGlobals();
		LuaC.install();
		PerformanceRunner.testRun("LuaJC", globals, false);
	}

	@Test
	public void testLuaJC() {
		LuaTable globals = PerformanceRunner.getGlobals();
		LuaJC.install();
		PerformanceRunner.testRun("LuaJC", globals, false);
	}
}
