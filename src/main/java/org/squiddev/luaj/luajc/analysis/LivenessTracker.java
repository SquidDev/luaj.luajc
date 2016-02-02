package org.squiddev.luaj.luajc.analysis;

import org.squiddev.luaj.luajc.analysis.block.BasicBlock;

import java.util.*;

/**
 * Checks
 */
public final class LivenessTracker {
	private final ProtoInfo info;
	private final Map<BasicBlock, Set<BasicBlock>> successors = new HashMap<BasicBlock, Set<BasicBlock>>();
	private final Map<SlotState, Boolean> active = new HashMap<SlotState, Boolean>();

	private final class SlotState {
		private final VarInfo info;
		private final int pc;

		private SlotState(VarInfo info, int pc) {
			this.info = info;
			this.pc = pc;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			SlotState slotState = (SlotState) o;
			return pc == slotState.pc && info.equals(slotState.info);
		}

		@Override
		public int hashCode() {
			return 31 * info.hashCode() + pc;
		}
	}

	public LivenessTracker(ProtoInfo info) {
		this.info = info;
	}

	/**
	 * Get all blocks that follow this one, including itself
	 *
	 * @param block The block to check
	 * @return All following blocks
	 */
	public Set<BasicBlock> getSuccessors(BasicBlock block) {
		Set<BasicBlock> next = successors.get(block);
		if (next == null) {
			next = new HashSet<BasicBlock>();
			addSuccessors(block, next);
			successors.put(block, Collections.unmodifiableSet(next));
		}
		return next;
	}

	private void addSuccessors(BasicBlock block, Set<BasicBlock> successors) {
		if (successors.add(block) && block.next != null) {
			for (BasicBlock next : block.next) {
				addSuccessors(next, successors);
			}
		}
	}

	/**
	 * Check if a variable is live at a specific point in the program
	 *
	 * @param var The variable to check
	 * @param pc  The pc to check at
	 * @return If it is live
	 */
	public boolean isLive(VarInfo var, int pc) {
		SlotState state = new SlotState(var, pc);

		Boolean live = this.active.get(state);
		if (live != null) return live;

		boolean result = isLiveUncached(var, pc);
		this.active.put(state, result);
		return result;
	}

	private boolean isLiveUncached(VarInfo var, int pc) {
		if (var == VarInfo.INVALID || !var.isReferenced) return false;

		BasicBlock varBlock = info.blocks[var.pc];
		BasicBlock pcBlock = info.blocks[pc];
		if (!varBlock.isLive || !pcBlock.isLive) return false;

		int size = var.phiReferences.size();
		int[] pcs = var.phiReferences.values();
		Set<BasicBlock> successors = null;

		for (int i = 0; i < size; i++) {
			int refPc = pcs[i];

			BasicBlock refBlock = info.blocks[refPc];
			if (!refBlock.isLive) continue;

			if (refBlock == pcBlock && refPc > pc) return true;

			if (successors == null) successors = getSuccessors(pcBlock);
			if (successors.contains(refBlock)) return true;
		}

		return false;
	}
}
