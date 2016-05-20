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
package org.squiddev.cobalt.luajc;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.compiler.LuaC;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.luajc.compilation.JavaLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Alternative version of LuaJC which fixes class names properly.
 * For instance chunk-name breaks in LuaJC
 */
public class LuaJC implements LoadState.LuaCompiler {
	private static final String NON_IDENTIFIER = "[^a-zA-Z0-9_]";

	private final CompileOptions options;

	public LuaJC(CompileOptions options) {
		this.options = options;
	}

	public LuaJC() {
		this.options = new CompileOptions();
	}

	public static void install(LuaState state, CompileOptions options) {
		state.compiler = new LuaJC(options);
	}

	public static void install(LuaState state) {
		state.compiler = new LuaJC();
	}

	@Override
	public LuaFunction load(InputStream stream, LuaString luaName, LuaValue env) throws IOException {
		Prototype p = LuaC.compile(stream, luaName);
		String name = luaName.toString();

		JavaLoader loader = new JavaLoader(LuaJC.class.getClassLoader(), options, toClassName(name), name);
		try {
			return loader.load(env, p);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public static String toClassName(String chunkName) {
		String stub = (chunkName.endsWith(".lua") ? chunkName.substring(0, chunkName.length() - 4) : chunkName);
		String className = stub.replace('/', '.').replaceAll(NON_IDENTIFIER, "_");

		int c = className.charAt(0);
		if (c != '_' && !Character.isJavaIdentifierStart(c)) className = "_" + className;

		return className;
	}
}
