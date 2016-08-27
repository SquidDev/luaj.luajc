package org.squiddev.cobalt.luajc.utils;


import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.Varargs;

public final class CompilationHelper {
	public static void rawsetList(LuaTable table, Varargs args, int offset) {
		int j = args.count();
		for (int i = 0; i < j; i++) {
			table.rawset(offset + i, args.arg(i + 1));
		}
	}
}
