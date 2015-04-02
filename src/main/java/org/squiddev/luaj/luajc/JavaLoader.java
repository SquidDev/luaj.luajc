/**
 * ****************************************************************************
 * Copyright (c) 2010 Luaj.org. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
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

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;

import java.util.HashMap;
import java.util.Map;

public class JavaLoader extends ClassLoader {
	/**
	 * Validate the sources on load
	 * This helps debug but will slow down compilation massively
	 */
	public boolean verifySources = false;

	/**
	 * The environment to load files from
	 */
	private final LuaValue env;

	private Map<String, byte[]> unloaded = new HashMap<>();
	private Map<String, Prototype> prototypes = new HashMap<>();

	public JavaLoader(LuaValue env) {
		super(JavaLoader.class.getClassLoader());
		this.env = env;
	}

	public LuaFunction load(Prototype p, String className, String filename) {
		JavaGen jg = new JavaGen(p, className, filename);
		return load(jg);
	}

	public LuaFunction load(JavaGen jg) {
		include(jg);
		return load(jg.className);
	}

	public LuaFunction load(String className) {
		try {
			Class c = loadClass(className);
			LuaFunction v = (LuaFunction) c.newInstance();
			v.setfenv(env);
			return v;
		} catch (Exception e) {
			throw new IllegalStateException("bad class gen: " + e.getMessage(), e);
		}
	}

	public void include(JavaGen jg) {
		unloaded.put(jg.className, jg.bytecode);
		prototypes.put(jg.className, jg.prototype);

		for (int i = 0, n = jg.inners != null ? jg.inners.length : 0; i < n; i++) {
			include(jg.inners[i]);
		}

		if (verifySources) {
			jg.validate(this);
		}
	}

	public Class findClass(String className) throws ClassNotFoundException {
		byte[] bytes = unloaded.get(className);
		if (bytes != null) {
			Class generatedClass = defineClass(className, bytes, 0, bytes.length);

			// Attempt to set the prototype object to this class
			try {
				generatedClass.getField(JavaBuilder.PROTOTYPE_NAME).set(null, prototypes.get(className));
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}

			return generatedClass;
		}
		return super.findClass(className);
	}
}
