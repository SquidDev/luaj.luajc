package org.squiddev.luaj.luajc;

import org.luaj.vm2.Prototype;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.DebugLib;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.compilation.JavaGen;
import org.squiddev.luaj.luajc.compilation.JavaLoader;
import org.squiddev.luaj.luajc.utils.AsmUtils;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import static org.squiddev.luaj.luajc.CompileOptions.THRESHOLD;
import static org.squiddev.luaj.luajc.LuaJC.toClassName;

public class DumpFunction {
	public static void main(String[] args) {
		String name;
		if (args.length != 1) {
			System.out.print("File> ");

			try {
				name = new BufferedReader(new InputStreamReader(System.in)).readLine();
			} catch (IOException e) {
				System.err.println(e.toString());
				System.exit(1);
				return;
			}
		} else {
			name = args[0];

		}

		DebugLib.DEBUG_ENABLED = false;

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(name);
			Prototype p = LuaC.compile(stream, name);

			String fileName = new File(name).getName();
			String klassName = toClassName(fileName);
			String prefix = new File(new File(name).getParentFile(), klassName).toString();
			JavaLoader loader = new JavaLoader(new CompileOptions("", THRESHOLD, true, null), klassName, fileName);
			ProtoInfo rootInfo = new ProtoInfo(p, loader);

			Queue<ProtoInfo> queue = new LinkedList<ProtoInfo>();
			queue.add(rootInfo);

			while (!queue.isEmpty()) {
				ProtoInfo info = queue.remove();
				System.out.println(prefix + info.name);
				if (info.subprotos != null) Collections.addAll(queue, info.subprotos);

				PrintWriter dumpWriter = null;
				OutputStream classWriter = null;
				try {
					dumpWriter = new PrintWriter(new FileOutputStream(prefix + info.name + ".dump"));
					classWriter = new FileOutputStream(prefix + info.name + ".class");
					dumpWriter.println(info);

					byte[] bytes = new JavaGen(info, loader, fileName).bytecode;
					AsmUtils.dump(bytes, dumpWriter);

					classWriter.write(bytes);
				} catch (RuntimeException e) {
					e.printStackTrace(dumpWriter);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (dumpWriter != null) dumpWriter.close();
					if (classWriter != null) classWriter.close();
				}
			}

		} catch (FileNotFoundException e) {
			System.err.println("Cannot find " + name + ": " + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			try {
				stream.close();
			} catch (IOException ignored) {
			}

			System.err.println(e.toString());
			System.exit(1);
		}
	}
}
