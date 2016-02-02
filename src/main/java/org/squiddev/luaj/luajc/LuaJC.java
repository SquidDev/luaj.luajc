/**
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
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
