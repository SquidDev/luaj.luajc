package org.squiddev.luaj.luajc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.squiddev.luaj.luajc.compilation.JavaLoader;
import org.squiddev.luaj.luajc.lasm.LasmParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

@RunWith(Parameterized.class)
public class LasmTest {
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getLua() {
		return Arrays.asList(new Object[][]{
			{"add"},
			{"closure"},
			{"label"},
		});
	}

	protected final String name;
	protected LuaValue globals;
	protected Prototype prototype;

	public LasmTest(String name) {
		this.name = name;
	}

	@Before
	public void setup() throws IOException {
		globals = LuaEnv.makeGlobals();

		InputStream stream = Loader.class.getResourceAsStream("/org/squiddev/luaj/luajc/lasm/" + name + ".lasm");
		if (stream == null) throw new IOException("Cannot load " + name);

		Scanner s = new Scanner(stream).useDelimiter("\\A");
		String contents = s.hasNext() ? s.next() : "";
		stream.close();

		prototype = new LasmParser(name, contents).parse();
	}

	@Test
	public void luaJC() throws Exception {
		JavaLoader loader = new JavaLoader(LuaJC.class.getClassLoader(), Loader.getOptions(1), LuaJC.toClassName(name + ".lasm"), name);
		loader.load(globals, prototype).call();
	}

	@Test
	public void luaC() throws Exception {
		new LuaClosure(prototype, globals).call();
	}
}
