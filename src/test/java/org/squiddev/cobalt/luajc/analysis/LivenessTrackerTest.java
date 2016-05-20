package org.squiddev.cobalt.luajc.analysis;

import org.junit.Test;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.luajc.CompileOptions;
import org.squiddev.cobalt.luajc.Loader;
import org.squiddev.cobalt.luajc.analysis.block.LivenessTracker;
import org.squiddev.cobalt.luajc.compilation.JavaLoader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LivenessTrackerTest {
	@Test
	public void testIsLive() throws Exception {
		Prototype proto = Loader.loadPrototype("analysis/liveness", "liveness.lua");
		JavaLoader loader = new JavaLoader(new CompileOptions(), "liveness", "liveness.lua");

		ProtoInfo info = new ProtoInfo(proto, loader);
		LivenessTracker tracker = new LivenessTracker(info);

		assertTrue(tracker.isLive(info.vars[0][0], 15));
		assertTrue(tracker.isLive(info.vars[0][0], 19));

		assertFalse(tracker.isLive(info.vars[11][2], 19));
		assertTrue(tracker.isLive(info.vars[11][2], 15));
	}
}
