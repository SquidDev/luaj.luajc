package org.squiddev.luaj.luajc;

import org.luaj.vm2.Lua;
import org.luaj.vm2.Prototype;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BasicBlock {
	/**
	 * Start PC of the block
	 */
	protected int pc0;

	/**
	 * End PC of the block
	 */
	protected int pc1;

	/**
	 * Previous blocks (blocks that jump to this one)
	 */
	protected BasicBlock[] prev;

	/**
	 * Following blocks (blocks that are jumped to from this one)
	 */
	protected BasicBlock[] next;

	/**
	 * If this block is used
	 */
	protected boolean isLive;

	public BasicBlock(int pc) {
		pc0 = pc1 = pc;
	}

	public String toString() {
		return (pc0 + 1) + "-" + (pc1 + 1)
			+ (prev != null ? "  prv: " + str(prev, 1) : "")
			+ (next != null ? "  nxt: " + str(next, 0) : "")
			+ "\n";
	}

	private String str(BasicBlock[] b, int p) {
		if (b == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i = 0, n = b.length; i < n; i++) {
			if (i > 0) sb.append(",");
			sb.append(String.valueOf(p == 1 ? b[i].pc1 + 1 : b[i].pc0 + 1));
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Find all blocks in a prototype
	 *
	 * @param p The prototype to find blocks in
	 * @return The list of resulting blocks
	 */
	public static BasicBlock[] findBasicBlocks(Prototype p) {
		// mark beginnings, endings
		final int n = p.code.length;
		final boolean[] isBeginning = new boolean[n];
		final boolean[] isEnd = new boolean[n];
		isBeginning[0] = true;

		// Mark beginning and end of blocks
		BranchVisitor bv = new BranchVisitor(isBeginning) {
			public void visitBranch(int pc0, int pc1) {
				isEnd[pc0] = true;
				isBeginning[pc1] = true;
			}

			public void visitReturn(int pc) {
				isEnd[pc] = true;
			}
		};

		visitBranches(p, bv); // 1st time to mark branches
		visitBranches(p, bv); // 2nd time to catch merges

		// Create the blocks
		final BasicBlock[] blocks = new BasicBlock[n];
		for (int i = 0; i < n; i++) {
			isBeginning[i] = true;
			BasicBlock b = new BasicBlock(i);
			blocks[i] = b;
			while (!isEnd[i] && i + 1 < n && !isBeginning[i + 1]) {
				blocks[b.pc1 = ++i] = b;
			}
		}

		// Count number of next and previous blocks
		final int[] nNext = new int[n];
		final int[] nPrevious = new int[n];
		visitBranches(p, new BranchVisitor(isBeginning) {
			public void visitBranch(int pc0, int pc1) {
				nNext[pc0]++;
				nPrevious[pc1]++;
			}
		});

		// Create the blocks and reference previous and next blocks
		visitBranches(p, new BranchVisitor(isBeginning) {
			public void visitBranch(int pc0, int pc1) {
				if (blocks[pc0].next == null) blocks[pc0].next = new BasicBlock[nNext[pc0]];
				if (blocks[pc1].prev == null) blocks[pc1].prev = new BasicBlock[nPrevious[pc1]];
				blocks[pc0].next[--nNext[pc0]] = blocks[pc1];
				blocks[pc1].prev[--nPrevious[pc1]] = blocks[pc0];
			}
		});
		return blocks;
	}

	/**
	 * Filter the list of blocks to only live blocks
	 *
	 * @param blocks The blocks to filter
	 * @return The filtered list of reachable blocks
	 */
	public static BasicBlock[] findLiveBlocks(BasicBlock[] blocks) {
		// Add all reachable blocks
		Queue<BasicBlock> next = new LinkedList<>();
		next.add(blocks[0]);

		// For each item find the next blocks and add them to the queue
		while (!next.isEmpty()) {
			BasicBlock b = next.remove();
			if (!b.isLive) {
				b.isLive = true;
				for (int i = 0, n = b.next != null ? b.next.length : 0; i < n; i++) {
					if (!b.next[i].isLive) {
						next.add(b.next[i]);
					}
				}
			}
		}

		// Create list in natural order
		List<BasicBlock> list = new ArrayList<>();
		for (int i = 0; i < blocks.length; i = blocks[i].pc1 + 1) {
			if (blocks[i].isLive) {
				list.add(blocks[i]);
			}
		}

		return list.toArray(new BasicBlock[list.size()]);
	}

	/**
	 * Helper class to visit branch instructions
	 */
	public abstract static class BranchVisitor {
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
	}

	public static void visitBranches(Prototype p, BranchVisitor visitor) {
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
					visitor.visitBranch(i, i + 2);
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
					visitor.visitBranch(i, branchTo);
					visitor.visitBranch(i, i + 1);
					continue;
				case Lua.OP_FORLOOP:
					branchOffset = Lua.GETARG_sBx(ins);
					branchTo = i + branchOffset + 1;
					visitor.visitBranch(i, branchTo);
					visitor.visitBranch(i, i + 1);
					continue;
				case Lua.OP_JMP:
				case Lua.OP_FORPREP:
					branchOffset = Lua.GETARG_sBx(ins);
					branchTo = i + branchOffset + 1;
					visitor.visitBranch(i, branchTo);
					continue;
				case Lua.OP_TAILCALL:
				case Lua.OP_RETURN:
					visitor.visitReturn(i);
					continue;
			}
			if (i + 1 < n && visitor.isBeginning[i + 1]) {
				visitor.visitBranch(i, i + 1);
			}
		}
	}
}
