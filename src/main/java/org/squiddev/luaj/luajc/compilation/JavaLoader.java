package org.squiddev.luaj.luajc.compilation;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.objectweb.asm.ClassWriter;
import org.squiddev.luaj.luajc.Constants;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.function.FunctionWrapper;
import org.squiddev.luaj.luajc.utils.AsmUtils;

import java.util.HashMap;
import java.util.Map;

public class JavaLoader extends ClassLoader {
	/**
	 * Validate the sources on load
	 * This helps debug but will slow down compilation massively
	 */
	public boolean verifySources = true;

	/**
	 * The prefix for all classes
	 */
	public final String prefix;

	/**
	 * The filename to load with
	 */
	public final String name;

	/**
	 * Lookup of classes that haven't been loaded yet
	 */
	private final Map<String, byte[]> unloaded = new HashMap<String, byte[]>();

	public JavaLoader(String prefix, String name) {
		this.prefix = prefix;
		this.name = name;
	}

	public JavaLoader(ClassLoader parent, String prefix, String name) {
		super(parent);
		this.prefix = prefix;
		this.name = name;
	}

	public FunctionWrapper load(LuaValue env, Prototype prototype) throws Exception {
		ProtoInfo info = new ProtoInfo(prototype);

		// Setup the prototype storage
		ClassWriter writer = PrototypeStorage.createStorage(prefix, info);
		writer.visitEnd();

		byte[] contents = writer.toByteArray();

		if (verifySources) AsmUtils.validateClass(contents, this);
		Class<?> klass = defineClass(prefix.replace('/', '.') + Constants.PROTOTYPE_STORAGE, contents);
		klass.getDeclaredMethod("setup", ProtoInfo.class).invoke(null, info);

		return new FunctionWrapper(info, env);
	}

	public void include(JavaGen jg) {
		unloaded.put(prefix.replace('/', '.') + jg.prototype.name, jg.bytecode);
		if (verifySources) jg.validate(this);
	}

	@Override
	public Class findClass(String className) throws ClassNotFoundException {
		byte[] bytes = unloaded.get(className);
		if (bytes != null) {
			return defineClass(className, bytes);
		}
		return super.findClass(className);
	}

	protected Class<?> defineClass(String className, byte[] bytes) {
		return defineClass(className, bytes, 0, bytes.length);
	}
}
