package org.squiddev.luaj.luajc.compilation;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.objectweb.asm.ClassWriter;
import org.squiddev.luaj.luajc.Constants;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.function.FunctionExecutor;
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
		ProtoInfo info = new ProtoInfo(prototype, this);

		// Setup the prototype storage
		ClassWriter writer = PrototypeStorage.createStorage(prefix, info);
		writer.visitEnd();

		Class<?> klass = defineClass(prefix.replace('/', '.') + Constants.PROTOTYPE_STORAGE, writer.toByteArray());
		klass.getDeclaredMethod("setup", ProtoInfo.class).invoke(null, info);

		return new FunctionWrapper(info, env);
	}

	public FunctionExecutor include(JavaGen jg) throws Exception {
		Class<?> klass = defineClass(prefix.replace('/', '.') + jg.prototype.name, jg.bytecode);
		return (FunctionExecutor) klass.getConstructor().newInstance();
	}

	public FunctionExecutor include(ProtoInfo info) {
		try {
			return include(new JavaGen(info, this, name));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Class<?> defineClass(String className, byte[] bytes) {
		if (verifySources) AsmUtils.validateClass(bytes, this);
		return defineClass(className, bytes, 0, bytes.length);
	}
}
