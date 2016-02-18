/**
 * ****************************************************************************
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2016 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.squiddev.luaj.luajc.analysis.block;

import org.luaj.vm2.Lua;
import org.luaj.vm2.Prototype;

/**
 * Helper class to visit branch instructions
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
	protected void visitBranch(int fromPc, int toPc) {
	}

	/**
	 * Visit a return instruction
	 *
	 * @param pc The instruction PC we are returning at
	 */
	protected void visitReturn(int pc) {
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
}
