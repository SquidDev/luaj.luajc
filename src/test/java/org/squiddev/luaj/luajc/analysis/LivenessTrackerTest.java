package org.squiddev.luaj.luajc.analysis;

import org.junit.Test;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.LuaC;
import org.squiddev.luaj.luajc.CompileOptions;
import org.squiddev.luaj.luajc.compilation.JavaLoader;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LivenessTrackerTest {
	public final String source = "local x = 2\n" +
		"local print = print\n" +
		"print(x + 1)\n" +
		"\n" +
		"if x == 2 then x = 3 end\n" +
		"\n" +
		"print(x + 1)\n" +
		"\n" +
		"local y = 2\n" +
		"if x == 2 then\n" +
		"\tprint(y)\n" +
		"end\n" +
		"\n" +
		"print(x) ";

	@Test
	public void testIsLive() throws Exception {
		Prototype proto = LuaC.compile(new ByteArrayInputStream(source.getBytes()), "testing");
		JavaLoader loader = new JavaLoader(new CompileOptions(), "testing", "testing.lua");

		ProtoInfo info = new ProtoInfo(proto, loader);
		LivenessTracker tracker = new LivenessTracker(info);

		assertTrue(tracker.isLive(info.vars[0][0], 15));
		assertTrue(tracker.isLive(info.vars[0][0], 19));

		assertFalse(tracker.isLive(info.vars[11][2], 19));
		assertTrue(tracker.isLive(info.vars[11][2], 15));
	}
}
