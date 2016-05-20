package org.squiddev.cobalt.luajc.compilation;

import org.objectweb.asm.ClassWriter;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.luajc.CompileOptions;
import org.squiddev.cobalt.luajc.analysis.ProtoInfo;
import org.squiddev.cobalt.luajc.function.FunctionExecutor;
import org.squiddev.cobalt.luajc.function.FunctionWrapper;
import org.squiddev.cobalt.luajc.function.executors.FallbackExecutor;
import org.squiddev.cobalt.luajc.utils.AsmUtils;

public class JavaLoader extends ClassLoader {
	/**
	 * Options for compilation
	 */
	public final CompileOptions options;

	/**
	 * The filename to load with
	 */
	public final String filename;

	public final String name;

	public JavaLoader(CompileOptions options, String name, String filename) {
		this.options = options;
		this.filename = filename;
		this.name = name;
	}

	public JavaLoader(ClassLoader parent, CompileOptions options, String name, String filename) {
		super(parent);
		this.options = options;
		this.filename = filename;
		this.name = name;
	}

	public FunctionWrapper load(LuaValue env, Prototype prototype) throws Exception {
		ProtoInfo info = new ProtoInfo(prototype, this);

		// Setup the prototype storage
		ClassWriter writer = PrototypeStorage.createStorage(options.prefix + name, info);
		writer.visitEnd();

		Class<?> klass = defineClass(options.dotPrefix + name.replace('/', '.') + Constants.PROTOTYPE_STORAGE, writer.toByteArray());
		klass.getDeclaredMethod("setup", ProtoInfo.class).invoke(null, info);

		return new FunctionWrapper(info, env);
	}

	public FunctionExecutor include(JavaGen jg) throws Exception {
		String wholeName = options.dotPrefix + name.replace('/', '.') + jg.prototype.name;

		Class<?> klass = defineClass(wholeName, jg.bytecode);
		return (FunctionExecutor) klass.getConstructor().newInstance();
	}

	public FunctionExecutor include(ProtoInfo info) {
		try {
			return include(new JavaGen(info, this, filename));
		} catch (RuntimeException e) {
			if (options.handler != null) {
				options.handler.handleError(info, e);
				return FallbackExecutor.INSTANCE;
			} else {
				throw e;
			}
		} catch (VerifyError e) {
			if (options.handler != null) {
				options.handler.handleError(info, e);
				return FallbackExecutor.INSTANCE;
			} else {
				throw e;
			}
		} catch (Exception e) {
			if (options.handler != null) {
				options.handler.handleError(info, e);
				return FallbackExecutor.INSTANCE;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	protected Class<?> defineClass(String className, byte[] bytes) {
		if (options.verify) AsmUtils.validateClass(bytes, this);
		return defineClass(className, bytes, 0, bytes.length);
	}
}
