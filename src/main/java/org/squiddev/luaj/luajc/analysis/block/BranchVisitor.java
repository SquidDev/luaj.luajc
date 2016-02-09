package org.squiddev.luaj.luajc.analysis.block;

import org.luaj.vm2.Lua;
import org.luaj.vm2.Prototype;

/**
 * Visits branches
 */
public abstract class BranchVisitor {
	/**
	 * Which PCs are the beginning to a block
	 */
	protected final boolean[] isBeginning;

	public BranchVisitor(boolean[] isBeginning) {
		this.isBeginning = isBeginning;
	}

	/**
	 * Visit a branch instruction
	 *
	 * @param fromPc The instruction PC we are branching from
	 * @param toPc   The instruction PC we are branching to
	 */
	public void visitBranch(int fromPc, int toPc) {
	}

	/**
	 * Visit a return instruction
	 *
	 * @param pc The instruction PC we are returning at
	 */
	public void visitReturn(int pc) {
	}

	public final void visitBranches(Prototype p) {
		int branchOffset, branchTo;
		int[] code = p.code;
		int n = code.length;
		for (int i = 0; i < n; i++) {
			int ins = code[i];
			switch (Lua.GET_OPCODE(ins)) {
				case Lua.OP_LOADBOOL:
					if (0 == Lua.GETARG_C(ins)) {
						break;
					}
					if (Lua.GET_OPCODE(code[i + 1]) == Lua.OP_JMP) {
						throw new IllegalArgumentException("OP_LOADBOOL followed by jump at " + i);
					}
					visitBranch(i, i + 2);
					continue;
				case Lua.OP_EQ:
				case Lua.OP_LT:
				case Lua.OP_LE:
				case Lua.OP_TEST:
				case Lua.OP_TESTSET:
				case Lua.OP_TFORLOOP:
					if (Lua.GET_OPCODE(code[i + 1]) != Lua.OP_JMP) {
						throw new IllegalArgumentException("test not followed by jump at " + i);
					}
					branchOffset = Lua.GETARG_sBx(code[i + 1]);
					++i;
					branchTo = i + branchOffset + 1;
					visitBranch(i, branchTo);
					visitBranch(i, i + 1);
					continue;
				case Lua.OP_FORLOOP:
					branchOffset = Lua.GETARG_sBx(ins);
					branchTo = i + branchOffset + 1;
					visitBranch(i, branchTo);
					visitBranch(i, i + 1);
					continue;
				case Lua.OP_JMP:
				case Lua.OP_FORPREP:
					branchOffset = Lua.GETARG_sBx(ins);
					branchTo = i + branchOffset + 1;
					visitBranch(i, branchTo);
					continue;
				case Lua.OP_TAILCALL:
				case Lua.OP_RETURN:
					visitReturn(i);
					continue;
			}
			if (i + 1 < n && isBeginning[i + 1]) {
				visitBranch(i, i + 1);
			}
		}
	}

	/**
	 * If an opcode is a branch op
	 *
	 * @param opcode The opcode to check
	 * @return If it is a branch op
	 */
	public static boolean isTerminator(int opcode) {
		return opcode == Lua.OP_JMP || opcode == Lua.OP_FORLOOP || opcode == Lua.OP_FORPREP || opcode == Lua.OP_RETURN || opcode == Lua.OP_TAILCALL;
	}
}
