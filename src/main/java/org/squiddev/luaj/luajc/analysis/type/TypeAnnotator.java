package org.squiddev.luaj.luajc.analysis.type;

import org.squiddev.luaj.luajc.analysis.PhiInfo;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.analysis.block.BasicBlock;

import java.util.*;

/**
 * Annotates variables
 */
public class TypeAnnotator {
	private final ProtoInfo info;
	private final Set<VarInfo> known = new HashSet<VarInfo>();

	public TypeAnnotator(ProtoInfo info) {
		this.info = info;
	}

	/**
	 * Fill variables with a type percentage over a threshold
	 *
	 * @param threshold The threshold
	 * @return Remaining untyped variables
	 */
	public Set<VarInfo> fillFromThreshold(double threshold) {
		Set<VarInfo> unknown = new HashSet<VarInfo>();

		for (VarInfo param : info.params) {
			if (!annotate(param, threshold)) unknown.add(param);
		}
		for (BasicBlock block : info.blockList) {
			for (VarInfo var : block.entry) {
				if (!annotate(var, threshold)) {
					if (var instanceof PhiInfo && ((PhiInfo) var).values() == null) {
						throw new NullPointerException();
					}
					unknown.add(var);
				}
			}

			for (int pc = block.pc0; pc <= block.pc1; pc++) {
				for (VarInfo var : info.vars[pc]) {
					if (!annotate(var, threshold)) unknown.add(var);
				}
			}
		}

		for (VarInfo var : info.phis) {
			if (!annotate(var, threshold)) unknown.add(var);
		}

		return unknown;
	}

	/**
	 * The variable to annotate
	 *
	 * @param var The threshold to set at
	 * @return If we know the type of the variable
	 */
	private boolean annotate(VarInfo var, double threshold) {
		if (var == null || var == VarInfo.INVALID) return true;
		if (var.type != null) {
			known.add(var);
			return true;
		}

		// If it is a mutable upvalue presume it is a value,
		if (var.upvalue != null && var.upvalue.readWrite) {
			var.type = BasicType.VALUE;
			known.add(var);
			return true;
		}

		int sum = var.booleanCount + var.numberCount + var.valueCount;

		// If we've never visited this instruction then skip for now
		if (sum == 0) {
			return false;
		}

		int countThreshold = (int) (sum * threshold);
		if (var.booleanCount > countThreshold) {
			var.type = BasicType.BOOLEAN;
			known.add(var);
		} else if (var.numberCount > countThreshold) {
			var.type = BasicType.NUMBER;
			known.add(var);
		} else {
			/*
				We presume it is just a value.
				Whilst might be worth considering adding this to the unannotated list,
				there is sufficient "entropy" in this type, that it isn't needed.
			*/
			var.type = BasicType.VALUE;
			known.add(var);
		}

		return true;
	}

	/**
	 * Set phi nodes types from its definitions
	 *
	 * @param unknown Unknown nodes. This is modified in place.
	 */
	public void propagatePhiForward(Set<VarInfo> unknown) {
		Iterator<VarInfo> iter = unknown.iterator();
		while (iter.hasNext()) {
			VarInfo var = iter.next();
			if (var instanceof PhiInfo) {
				PhiInfo phi = (PhiInfo) var;

				BasicType type = null;
				for (VarInfo child : phi.values()) {
					BasicType childType = child.type;
					if (childType != null) {
						if (type == null) {
							type = childType;
						} else if (childType != type) {
							type = null;
							break;
						}
					}
				}

				if (type != null) {
					var.type = type;
					known.add(var);
					iter.remove();
				}
			}
		}
	}

	/**
	 * Set phi nodes definitions from its type
	 *
	 * @param unknown Unset nodes. This is modified in place.
	 */
	public void propagatePhisBackward(Set<VarInfo> unknown) {
		List<VarInfo> additional = new ArrayList<VarInfo>();
		for (VarInfo var : known) {

			// If this is a phi node
			if (var instanceof PhiInfo) {
				PhiInfo phi = (PhiInfo) var;

				// Then propagate its type to its definitions
				for (VarInfo definition : phi.values()) {
					if (unknown.contains(definition)) {
						definition.type = var.type;
						additional.add(var);
						unknown.remove(definition);
					}
				}
			}
		}

		known.addAll(additional);
	}

	public void fillUnknown(Set<VarInfo> unknown) {
		for (VarInfo var : unknown) {
			var.type = BasicType.VALUE;
			known.add(var);
		}
	}

	public void fill(double threshold) {
		Set<VarInfo> unknown = fillFromThreshold(threshold);
		propagatePhiForward(unknown);
		propagatePhisBackward(unknown);
		fillUnknown(unknown);
	}
}
