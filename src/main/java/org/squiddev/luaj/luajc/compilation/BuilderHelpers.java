package org.squiddev.luaj.luajc.compilation;

import org.luaj.vm2.Lua;
import org.squiddev.luaj.luajc.analysis.type.BasicType;

import static org.objectweb.asm.Opcodes.*;

/**
 * org.squiddev.luaj.luajc.compilation (luaj.luajc
 */
public final class BuilderHelpers {
	private BuilderHelpers() {
	}

	public static int getStoreOpcode(BasicType type) {
		switch (type) {
			case BOOLEAN:
				return ISTORE;
			case NUMBER:
				return DSTORE;
			case VALUE:
			default:
				return ASTORE;
		}
	}

	public static int getLoadOpcode(BasicType type) {
		switch (type) {
			case BOOLEAN:
				return ILOAD;
			case NUMBER:
				return DLOAD;
			case VALUE:
			default:
				return ALOAD;
		}
	}

	public static String getOpName(int op) {
		switch (op) {
			case Lua.OP_ADD:
				return "add";
			case Lua.OP_SUB:
				return "sub";
			case Lua.OP_MUL:
				return "mul";
			case Lua.OP_DIV:
				return "div";
			case Lua.OP_MOD:
				return "mod";
			case Lua.OP_POW:
				return "pow";
			case Lua.OP_UNM:
				return "min";
			case Lua.OP_NOT:
				return "not";
			case Lua.OP_LEN:
				return "len";
			case Lua.OP_EQ:
				return "eq_b";
			case Lua.OP_LT:
				return "lt_b";
			case Lua.OP_LE:
				return "lteq_b";
			default:
				throw new IllegalArgumentException("Unknown instruction " + op);
		}
	}

	public static int getOpcode(int op) {
		switch (op) {
			case Lua.OP_ADD:
				return DADD;
			case Lua.OP_SUB:
				return DSUB;
			case Lua.OP_MUL:
				return DMUL;
			case Lua.OP_DIV:
				return DDIV;
			case Lua.OP_UNM:
				return DNEG;
			case Lua.OP_MOD:
			case Lua.OP_POW:
			case Lua.OP_NOT:
				throw new IllegalArgumentException("MOD, POW and OP_NOT must be done manually");
			default:
				throw new IllegalArgumentException("Unknown instruction " + op);
		}
	}
}
