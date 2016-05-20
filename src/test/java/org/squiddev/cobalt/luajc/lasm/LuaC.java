package org.squiddev.cobalt.luajc.lasm;

import static org.squiddev.cobalt.Lua.*;

/**
 * Helper for LASM parser
 */
public class LuaC {
	public static int SET_OPCODE(int i, int o) {
		return (i & (MASK_NOT_OP)) | ((o << POS_OP) & MASK_OP);
	}

	public static int SETARG_A(int i, int u) {
		return (i & (MASK_NOT_A)) | ((u << POS_A) & MASK_A);
	}

	public static int SETARG_B(int i, int u) {
		return (i & (MASK_NOT_B)) | ((u << POS_B) & MASK_B);
	}

	public static int SETARG_C(int i, int u) {
		return (i & (MASK_NOT_C)) | ((u << POS_C) & MASK_C);
	}

	public static int SETARG_Bx(int i, int u) {
		return (i & (MASK_NOT_Bx)) | ((u << POS_Bx) & MASK_Bx);
	}

	public static int SETARG_sBx(int i, int u) {
		return SETARG_Bx(i, u + MAXARG_sBx);
	}

	public static int CREATE_ABC(int o, int a, int b, int c) {
		return ((o << POS_OP) & MASK_OP) |
			((a << POS_A) & MASK_A) |
			((b << POS_B) & MASK_B) |
			((c << POS_C) & MASK_C);
	}

	public static int CREATE_ABx(int o, int a, int bc) {
		return ((o << POS_OP) & MASK_OP) |
			((a << POS_A) & MASK_A) |
			((bc << POS_Bx) & MASK_Bx);
	}

	public static int CREATE_AsBx(int o, int A, int sBx) {
		return CREATE_ABx(o, A, sBx + MAXARG_sBx);
	}
}
