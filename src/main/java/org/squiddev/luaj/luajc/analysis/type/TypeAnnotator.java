package org.squiddev.luaj.luajc.analysis.type;

import org.squiddev.luaj.luajc.analysis.PhiInfo;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;

import java.util.*;

/**
 * Annotates variables
 */
public class TypeAnnotator {
	private final ProtoInfo info;
	private final Map<VarInfo, TypeInformation> types = new HashMap<VarInfo, TypeInformation>();

	public TypeAnnotator(ProtoInfo info) {
		this.info = info;
	}

	/**
	 * Fill variables with a type percentage over a threshold
	 *
	 * @param threshold The threshold
	 * @return Remaining unannotated types
	 */
	public Set<VarInfo> fillFromThreshold(double threshold) {
		Set<VarInfo> unannotated = new HashSet<VarInfo>();

		for (VarInfo[] stack : info.vars) {
			for (VarInfo var : stack) {
				// If it is a mutable upvalue presume it is a value,
				if (var.upvalue != null && var.upvalue.readWrite) {
					types.put(var, new TypeInformation(BasicType.VALUE));
					continue;
				}

				int sum = var.booleanCount + var.numberCount + var.valueCount;

				// If we've never visited this instruction then skip for now
				if (sum == 0) {
					unannotated.add(var);
					continue;
				}

				int countThreshold = (int) (sum * threshold);
				if (var.booleanCount > countThreshold) {
					types.put(var, new TypeInformation(BasicType.BOOLEAN));
				} else if (var.numberCount > countThreshold) {
					types.put(var, new TypeInformation(BasicType.NUMBER));
				} else {
					// We presume it is just a value.
					// It might be worth considering adding this to the unannotated list
					types.put(var, new TypeInformation(BasicType.VALUE));
				}
			}
		}

		return unannotated;
	}

	/**
	 * Set phi nodes types from its definitions
	 *
	 * @param unset Unset nodes. This is modified in place.
	 */
	public void propagatePhiForward(Set<VarInfo> unset) {
		Iterator<VarInfo> iter = unset.iterator();
		while (iter.hasNext()) {
			VarInfo var = iter.next();
			if (var instanceof PhiInfo) {
				PhiInfo phi = (PhiInfo) var;

				BasicType type = null;
				for (VarInfo child : phi.values()) {
					TypeInformation childType = types.get(child);
					if (childType != null) {
						if (type == null) {
							type = childType.type;
						} else if (childType.type != type) {
							type = null;
							break;
						}
					}
				}

				if (type != null) {
					types.put(var, new TypeInformation(type));
					iter.remove();
				}
			}
		}
	}

	/**
	 * Set phi nodes definitions from its type
	 *
	 * @param unset Unset nodes. This is modified in place.
	 */
	public void propagatePhisBackward(Set<VarInfo> unset) {
		for (Map.Entry<VarInfo, TypeInformation> entry : types.entrySet()) {
			VarInfo var = entry.getKey();

			// If this is a phi node
			if (var instanceof PhiInfo) {
				PhiInfo phi = (PhiInfo) var;

				// Then propagate its type to its definitions
				for (VarInfo definition : phi.values()) {
					if (unset.contains(definition)) {
						types.put(definition, new TypeInformation(entry.getValue().type));
						unset.remove(definition);
					}
				}
			}
		}
	}

	public TypeInformation getInformation(VarInfo var) {
		return types.get(var);
	}

	public BasicType getType(VarInfo var) {
		TypeInformation type = types.get(var);
		return type == null ? BasicType.VALUE : type.type;
	}
}
