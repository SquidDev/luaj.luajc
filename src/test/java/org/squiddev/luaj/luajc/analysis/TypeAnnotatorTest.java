package org.squiddev.luaj.luajc.analysis;

import org.junit.Test;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.luajc.CompileOptions;
import org.squiddev.luaj.luajc.Loader;
import org.squiddev.luaj.luajc.analysis.type.BasicType;
import org.squiddev.luaj.luajc.analysis.type.ConversionAnnotator;
import org.squiddev.luaj.luajc.analysis.type.TypeAnnotator;
import org.squiddev.luaj.luajc.compilation.JavaLoader;
import org.squiddev.luaj.luajc.function.FunctionWrapper;

import static org.junit.Assert.assertEquals;

public class TypeAnnotatorTest {
	@Test
	public void testAnnotations() throws Exception {
		LuaValue globals = JsePlatform.debugGlobals();
		Prototype proto = Loader.loadPrototype("analysis/types", "types.lua");
		JavaLoader loader = new JavaLoader(new CompileOptions(), "types", "types.lua");

		ProtoInfo info = new ProtoInfo(proto, loader);

		new FunctionWrapper(info, globals).call();

		ProtoInfo typed = info.subprotos[0];
		new TypeAnnotator(typed).fill(0.7f);

		// Initial arguments
		assertEquals(BasicType.VALUE, typed.vars[0][0].type);
		assertEquals(BasicType.VALUE, typed.vars[0][1].type);
		assertEquals(BasicType.BOOLEAN, typed.vars[0][2].type);

		// Once done x or 0
		assertEquals(BasicType.NUMBER, typed.vars[6][0].type);
		assertEquals(BasicType.NUMBER, typed.vars[6][1].type);
		assertEquals(BasicType.BOOLEAN, typed.vars[6][2].type);

		// After if statement
		assertEquals(BasicType.NUMBER, typed.vars[9][0].type);
	}

	@Test
	public void testConversions() throws Exception {
		LuaValue globals = JsePlatform.debugGlobals();
		Prototype proto = Loader.loadPrototype("analysis/types", "types.lua");
		JavaLoader loader = new JavaLoader(new CompileOptions(), "types", "types.lua");

		ProtoInfo info = new ProtoInfo(proto, loader);

		new FunctionWrapper(info, globals).call();

		ProtoInfo typed = info.subprotos[0];
		new TypeAnnotator(typed).fill(0.7f);
		new ConversionAnnotator(typed).fill();

		System.out.println(typed);
	}
}
