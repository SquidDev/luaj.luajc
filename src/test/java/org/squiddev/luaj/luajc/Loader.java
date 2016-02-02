package org.squiddev.luaj.luajc;

import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.LuaC;

import java.io.IOException;
import java.io.InputStream;

public final class Loader {
	public static InputStream load(String path) throws IOException {
		InputStream stream = Loader.class.getResourceAsStream("/org/squiddev/luaj/luajc/" + path + ".lua");
		if (stream == null) throw new IOException("Cannot load " + path);

		return stream;
	}

	public static Prototype loadPrototype(String path, String name) throws IOException {
		return LuaC.compile(load(path), name);
	}
}
