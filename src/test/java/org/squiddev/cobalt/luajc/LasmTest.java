package org.squiddev.cobalt.luajc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.function.LuaInterpreter;
import org.squiddev.cobalt.luajc.compilation.JavaLoader;
import org.squiddev.cobalt.luajc.lasm.LasmParser;

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
	private Prototype prototype;
	private LuaTable globals;
	private LuaState state;

	public LasmTest(String name) {
		this.name = name;
	}

	@Before
	public void setup() throws IOException {
		state = LuaEnv.makeState();
		globals = LuaEnv.makeGlobals(state);

		InputStream stream = Loader.class.getResourceAsStream("/org/squiddev/cobalt/luajc/lasm/" + name + ".lasm");
		if (stream == null) throw new IOException("Cannot load " + name);

		Scanner s = new Scanner(stream).useDelimiter("\\A");
		String contents = s.hasNext() ? s.next() : "";
		stream.close();

		prototype = new LasmParser(name, contents).parse();
	}

	@Test
	public void luaJC() throws Exception {
		JavaLoader loader = new JavaLoader(LuaJC.class.getClassLoader(), Loader.getOptions(1), LuaJC.toClassName(name + ".lasm"), name);
		loader.load(globals, prototype).call(state);
	}

	@Test
	public void luaC() throws Exception {
		new LuaInterpreter(prototype, globals).call(state);
	}
}
