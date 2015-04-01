package org.squiddev.luaj.luajc;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the performance between LuaC and LuaJC
 */
@Ignore
public class PerformanceTest {
	@Test
	public void testLuaC() {
		PerformanceRunner.testLuaC();
	}

	@Test
	public void testLuaJC() {
		PerformanceRunner.testLuaJC();
	}
}
