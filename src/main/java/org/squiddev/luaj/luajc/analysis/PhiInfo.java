package org.squiddev.luaj.luajc.analysis;

import org.squiddev.luaj.luajc.analysis.block.BasicBlock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This stores a list of potential variables this could resolve to
 * This is created as a result of multiple blocks
 */
public final class PhiInfo extends VarInfo {
	private final ProtoInfo pi;

	/**
	 * A list of variables this could resolve to
	 */
	protected Set<VarInfo> values;

	/**
	 * Create a new {@link PhiInfo}
	 *
	 * @param pi   The prototype this variable is part of
	 * @param slot The slot the variable is assigned to
	 * @param pc   The PC this variable is used at
	 */
	public PhiInfo(ProtoInfo pi, int slot, int pc) {
		super(slot, pc);
		this.pi = pi;
	}

	@Override
	public boolean isPhiVar() {
		return true;
	}

	public String toString() {
		return super.toString() + "={" + values + "}";
	}

	/**
	 * Return replacement variable if there is exactly one value possible,
	 * otherwise compute entire collection of variables and return null.
	 * Computes the list of all variable values, and saves it for the future.
	 * <p>
	 * This will set all child variables' isReferenced if this variable is
	 *
	 * @return new Variable to replace with if there is only one value, or null to leave alone.
	 * @see #isReferenced
	 */
	@Override
	public VarInfo resolvePhiVariableValues() {
		Set<BasicBlock> visitedBlocks = new HashSet<BasicBlock>();
		Set<VarInfo> vars = new HashSet<VarInfo>();
		collectUniqueValues(visitedBlocks, vars);

		if (vars.contains(INVALID)) return INVALID;

		if (vars.size() == 1) {
			VarInfo v = vars.iterator().next();
			v.isReferenced |= isReferenced;
			v.references.add(references);
			return v;
		}

		values = Collections.unmodifiableSet(vars);
		for (VarInfo v : vars) {
			v.isReferenced |= isReferenced;
			v.references.add(references);
		}

		return null;
	}

	/**
	 * Used to create unique variables
	 *
	 * @param visitedBlocks The list of blocks already visited
	 * @param vars          The list of unique variables
	 */
	@Override
	protected void collectUniqueValues(Set<BasicBlock> visitedBlocks, Set<VarInfo> vars) {
		BasicBlock b = pi.blocks[pc];
		if (pc == 0) {
			vars.add(pi.params[slot]);
		}
		for (int i = 0, n = b.prev != null ? b.prev.length : 0; i < n; i++) {
			BasicBlock bp = b.prev[i];
			if (visitedBlocks.add(bp)) {
				VarInfo v = pi.vars[bp.pc1][slot];
				if (v != null) {
					v.collectUniqueValues(visitedBlocks, vars);
				}
			}
		}
	}
}
