package org.squiddev.luaj.luajc;

import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.LuaC;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.utils.ValidationException;

import java.io.*;

public final class Loader {
	private static final File root = new File("build/test-out");
	private static final ErrorHandler handler = new ErrorHandler() {
		@Override
		public void handleError(ProtoInfo info, Throwable throwable) {
			if (throwable instanceof ValidationException) {
				if (!root.exists() && !root.mkdirs()) {
					throw new IllegalStateException("Cannot create " + handler);
				}

				ValidationException validation = (ValidationException) throwable;

				File file = new File(root, info.loader.name + info.name + ".dump");
				try {
					PrintWriter writer = new PrintWriter(new FileWriter(file));
					validation.printStackTrace(writer);
					writer.println(info);
					writer.println(validation.getTrace());
					writer.println(validation.getClassDump());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (throwable instanceof RuntimeException) {
				throw (RuntimeException) throwable;
			} else if (throwable instanceof Error) {
				throw (Error) throwable;
			} else {
				throw new RuntimeException(throwable);
			}
		}
	};

	public static void install(int threshold) {
		LuaJC.install(new CompileOptions(CompileOptions.PREFIX, threshold, CompileOptions.TYPE_THRESHOLD, true, handler));
	}

	public static InputStream load(String path) throws IOException {
		InputStream stream = Loader.class.getResourceAsStream("/org/squiddev/luaj/luajc/" + path + ".lua");
		if (stream == null) throw new IOException("Cannot load " + path);

		return stream;
	}

	public static Prototype loadPrototype(String path, String name) throws IOException {
		return LuaC.compile(load(path), name);
	}
}
