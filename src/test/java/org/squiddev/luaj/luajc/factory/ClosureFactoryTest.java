package org.squiddev.luaj.luajc.factory;

import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.luajc.function.CompilingClosure;
import org.squiddev.luaj.luajc.upvalue.AbstractUpvalue;
import org.squiddev.luaj.luajc.upvalue.ProxyUpvalue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ClosureFactoryTest {
	@Before
	public void setup() {
		JsePlatform.debugGlobals();
	}

	@Test
	public void createAbstract() throws Exception {
		ClosureFactoryLoader loader = new ClosureFactoryLoader();
		Class<?> klass = loader.loadAbstract(true, false);

		assertNotNull(klass.getMethod(ClosureFactoryBuilder.BUILD_NAME, LuaValue.class, AbstractUpvalue.class, LuaValue.class));
	}

	@Test
	public void createClosure() throws Exception {
		ClosureFactoryLoader loader = new ClosureFactoryLoader();
		Class<?> klass = loader.loadClosure(true, false);

		Constructor<?> ctor = klass.getConstructor(Prototype.class);
		Prototype proto = new Prototype();
		proto.nups = 2;

		Object obj = ctor.newInstance(proto);

		Method build = klass.getMethod(ClosureFactoryBuilder.BUILD_NAME, LuaValue.class, AbstractUpvalue.class, LuaValue.class);

		LuaValue env = LuaString.valueOf("20");
		ProxyUpvalue proxyUp = new ProxyUpvalue(null);
		LuaValue valueUp = LuaString.valueOf("30");
		CompilingClosure closure = (CompilingClosure) build.invoke(obj, env, proxyUp, valueUp);

		assertEquals(env, closure.getfenv());
		assertEquals(proxyUp, closure.upvalues[0]);
	}
}
