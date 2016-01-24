package org.squiddev.luaj.luajc.factory;

import org.objectweb.asm.ClassWriter;
import org.squiddev.luaj.luajc.utils.AsmUtils;

import java.util.HashSet;

import static org.squiddev.luaj.luajc.Constants.DOT_PREFIX;
import static org.squiddev.luaj.luajc.factory.ClosureFactoryBuilder.*;

/**
 * A loader for {@link ClosureFactoryBuilder}
 */
public final class ClosureFactoryLoader extends ClassLoader {
	private final HashSet<String> loaded = new HashSet<String>();

	public ClosureFactoryLoader(ClassLoader parent) {
		super(parent);
	}

	public ClosureFactoryLoader() {
	}

	public void ensureAbstractLoaded(boolean... upvalues) {
		String name = DOT_PREFIX + FACTORY_NAME + getModifierName(upvalues);
		if (loaded.add(name)) {
			ClassWriter writer = buildAbstractFactory(upvalues);
			writer.visitEnd();
			defineClass(name, writer.toByteArray());
		}
	}

	public void ensureClosureLoaded(boolean... upvalues) {
		ensureAbstractLoaded(upvalues);

		String name = DOT_PREFIX + CLOSURE_NAME + getModifierName(upvalues);
		if (loaded.add(name)) {
			ClassWriter writer = buildClosureFactory(upvalues);
			writer.visitEnd();
			defineClass(name, writer.toByteArray());
		}
	}

	public Class<?> loadAbstract(boolean... upvalues) throws ClassNotFoundException {
		ensureAbstractLoaded(upvalues);
		return loadClass(DOT_PREFIX + FACTORY_NAME + getModifierName(upvalues));
	}

	public Class<?> loadClosure(boolean... upvalues) throws ClassNotFoundException {
		ensureClosureLoaded(upvalues);
		return loadClass(DOT_PREFIX + CLOSURE_NAME + getModifierName(upvalues));
	}


	protected void defineClass(String name, byte[] contents) {
		AsmUtils.validateClass(contents, this);
		defineClass(name, contents, 0, contents.length);
	}

}
