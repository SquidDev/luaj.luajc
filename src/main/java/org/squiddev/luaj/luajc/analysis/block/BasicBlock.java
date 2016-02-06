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

import org.luaj.vm2.Prototype;
import org.squiddev.luaj.luajc.analysis.VarInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public final class BasicBlock {
	/**
	 * Start PC of the block
	 */
	public final int pc0;

	/**
	 * End PC of the block
	 */
	public int pc1;

	/**
	 * Previous blocks (blocks that jump to this one)
	 */
	public BasicBlock[] prev;

	/**
	 * Following blocks (blocks that are jumped to from this one)
	 */
	public BasicBlock[] next;

	/**
	 * If this block is used
	 */
	public boolean isLive;

	/**
	 * The dominator for this block
	 */
	public BasicBlock dominator;

	/**
	 * Variables at block entry
	 */
	public final VarInfo[] entry;


	public BasicBlock(int pc, int maxStack) {
		pc0 = pc1 = pc;
		entry = new VarInfo[maxStack];
	}

	@Override
	public String toString() {
		return (pc0 + 1) + "-" + (pc1 + 1)
			+ (prev != null ? "  prv: " + str(prev, 1) : "")
			+ (next != null ? "  nxt: " + str(next, 0) : "");
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
	 * Check if this block dominates another
	 *
	 * @param block The block that may be dominated
	 * @return If this block dominates the other one
	 */
	public boolean dominates(BasicBlock block) {
		while (block != null) {
			if (block == this) return true;
			block = block.dominator;
		}

		return false;
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
		final int stack = p.maxstacksize;
		final boolean[] isBeginning = new boolean[n];
		final boolean[] isEnd = new boolean[n];
		isBeginning[0] = true;

		// Mark beginning and end of blocks
		BranchVisitor bv = new BranchVisitor(isBeginning) {
			@Override
			public void visitBranch(int pc0, int pc1) {
				isEnd[pc0] = true;
				isBeginning[pc1] = true;
			}

			@Override
			public void visitReturn(int pc) {
				isEnd[pc] = true;
			}
		};

		bv.visitBranches(p); // 1st time to mark branches
		bv.visitBranches(p); // 2nd time to catch merges

		// Create the blocks
		final BasicBlock[] blocks = new BasicBlock[n];
		for (int i = 0; i < n; i++) {
			isBeginning[i] = true;
			BasicBlock b = new BasicBlock(i, stack);
			blocks[i] = b;
			while (!isEnd[i] && i + 1 < n && !isBeginning[i + 1]) {
				blocks[b.pc1 = ++i] = b;
			}
		}

		// Count number of next and previous blocks
		final int[] nNext = new int[n];
		final int[] nPrevious = new int[n];
		new BranchVisitor(isBeginning) {
			@Override
			public void visitBranch(int pc0, int pc1) {
				nNext[pc0]++;
				nPrevious[pc1]++;
			}
		}.visitBranches(p);

		// Create the blocks and reference previous and next blocks
		new BranchVisitor(isBeginning) {
			@Override
			public void visitBranch(int pc0, int pc1) {
				if (blocks[pc0].next == null) blocks[pc0].next = new BasicBlock[nNext[pc0]];
				if (blocks[pc1].prev == null) blocks[pc1].prev = new BasicBlock[nPrevious[pc1]];
				blocks[pc0].next[--nNext[pc0]] = blocks[pc1];
				blocks[pc1].prev[--nPrevious[pc1]] = blocks[pc0];
			}
		}.visitBranches(p);
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
		Queue<BasicBlock> next = new LinkedList<BasicBlock>();
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
		List<BasicBlock> list = new ArrayList<BasicBlock>();
		for (int i = 0; i < blocks.length; i = blocks[i].pc1 + 1) {
			if (blocks[i].isLive) {
				list.add(blocks[i]);
			}
		}

		return list.toArray(new BasicBlock[list.size()]);
	}
}
