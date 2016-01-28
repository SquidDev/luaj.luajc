package org.squiddev.luaj.luajc;

import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.LuaC;
import org.squiddev.luaj.luajc.compilation.JavaLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Alternative version of LuaJC which fixes class names properly.
 * For instance chunk-name breaks in LuaJC
 */
public class LuaJC implements LoadState.LuaCompiler {
	protected static final String NON_IDENTIFIER = "[^a-zA-Z0-9_]";

	private CompileOptions options;

	protected static LuaJC instance;

	public LuaJC() {
		this.options = new CompileOptions();
	}

	public static LuaJC getInstance() {
		if (instance == null) instance = new LuaJC();
		return instance;
	}

	/**
	 * Install the compiler as the main compiler to use.
	 */
	public static void install(CompileOptions options) {
		LoadState.compiler = getInstance();
		instance.options = options;
	}

	/**
	 * Install the compiler as the main compiler to use.
	 */
	public static void install() {
		install(new CompileOptions());
	}

	@Override
	public LuaFunction load(InputStream stream, String name, LuaValue env) throws IOException {
		Prototype p = LuaC.compile(stream, name);

		JavaLoader loader = new JavaLoader(LuaJC.class.getClassLoader(), options, toStandardJavaClassName(name), name);
		try {
			return loader.load(env, p);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private static String toStandardJavaClassName(String chunkName) {
		String stub = (chunkName.endsWith(".lua") ? chunkName.substring(0, chunkName.length() - 4) : chunkName);
		String className = stub.replace('/', '.').replaceAll(NON_IDENTIFIER, "_");

		int c = className.charAt(0);
		if (c != '_' && !Character.isJavaIdentifierStart(c)) className = "_" + className;

		return className;
	}
}
