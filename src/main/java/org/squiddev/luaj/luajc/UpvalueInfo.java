package org.squiddev.luaj.luajc;

import org.luaj.vm2.Lua;

public final class UpvalueInfo {
	/**
	 * The prototype the upvalue is defined in
	 */
	private final ProtoInfo pi;

	/**
	 * The slot the upvalue is assigned to
	 */
	private final int slot;

	/**
	 * Number of vars involved
	 */
	private int nvars;

	/**
	 * List of variables involved
	 */
	private VarInfo var[];

	/**
	 * Is this upvalue read-write
	 */
	public boolean readWrite;

	/**
	 * Create a new upvalue info
	 *
	 * @param pi   The prototype to use
	 * @param pc   The current program counter
	 * @param slot The slot of the upvalue
	 */
	public UpvalueInfo(ProtoInfo pi, int pc, int slot) {
		this.pi = pi;
		this.slot = slot;
		nvars = 0;
		var = null;
		includeVarAndPosteriorVars(pi.vars[slot][pc]);
		for (int i = 0; i < nvars; i++) {
			var[i].allocUpvalue = testIsAllocUpvalue(var[i]);
		}
		readWrite = nvars > 1;
	}

	/**
	 * Find variables that this upvalue links to
	 *
	 * @param var The variable to use
	 */
	private boolean includeVarAndPosteriorVars(VarInfo var) {
		if (var == null || var == VarInfo.INVALID) {
			return false;
		}
		if (var.upvalue == this) {
			return true;
		}
		var.upvalue = this;
		appendVar(var);
		if (isLoopVariable(var)) {
			return false;
		}
		boolean loopDetected = includePosteriorVarsCheckLoops(var);
		if (loopDetected) {
			includePriorVarsIgnoreLoops(var);
		}
		return loopDetected;
	}

	/**
	 * Checks if this {@link VarInfo} is a loop variable
	 *
	 * @param var The variable to check
	 * @return If this variable is one used in a loop
	 */
	private boolean isLoopVariable(VarInfo var) {
		if (var.pc >= 0) {
			switch (Lua.GET_OPCODE(pi.prototype.code[var.pc])) {
				case Lua.OP_TFORLOOP:
				case Lua.OP_FORLOOP:
					return true;
			}
		}
		return false;
	}

	private boolean includePosteriorVarsCheckLoops(VarInfo prior) {
		boolean loopDetected = false;
		for (BasicBlock b : pi.blockList) {
			VarInfo var = pi.vars[slot][b.pc1];
			if (var == prior) {
				for (int j = 0, m = b.next != null ? b.next.length : 0; j < m; j++) {
					BasicBlock b1 = b.next[j];
					VarInfo v1 = pi.vars[slot][b1.pc0];
					if (v1 != prior) {
						loopDetected |= includeVarAndPosteriorVars(v1);
						if (v1.isPhiVar()) {
							includePriorVarsIgnoreLoops(v1);
						}
					}
				}
			} else {
				for (int pc = b.pc1 - 1; pc >= b.pc0; pc--) {
					if (pi.vars[slot][pc] == prior) {
						loopDetected |= includeVarAndPosteriorVars(pi.vars[slot][pc + 1]);
						break;
					}
				}
			}
		}
		return loopDetected;
	}

	private void includePriorVarsIgnoreLoops(VarInfo poster) {
		for (BasicBlock b : pi.blockList) {
			VarInfo var = pi.vars[slot][b.pc0];
			if (var == poster) {
				for (int j = 0, m = b.prev != null ? b.prev.length : 0; j < m; j++) {
					BasicBlock b0 = b.prev[j];
					VarInfo v0 = pi.vars[slot][b0.pc1];
					if (v0 != poster) {
						includeVarAndPosteriorVars(v0);
					}
				}
			} else {
				for (int pc = b.pc0 + 1; pc <= b.pc1; pc++) {
					if (pi.vars[slot][pc] == poster) {
						includeVarAndPosteriorVars(pi.vars[slot][pc - 1]);
						break;
					}
				}
			}
		}
	}

	/**
	 * Add a variable to the list
	 *
	 * @param v The variable to add
	 */
	private void appendVar(VarInfo v) {
		if (nvars == 0) {
			var = new VarInfo[1];
		} else if (nvars + 1 >= var.length) {
			VarInfo[] s = var;
			var = new VarInfo[nvars * 2 + 1];
			System.arraycopy(s, 0, var, 0, nvars);
		}
		var[nvars++] = v;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(pi.name);
		for (int i = 0; i < nvars; i++) {
			sb.append(i > 0 ? "," : " ");
			sb.append(String.valueOf(var[i]));
		}
		if (readWrite) {
			sb.append("(rw)");
		}
		return sb.toString();
	}

	/**
	 * Test if this is an upvalue allocation
	 *
	 * @param var The variable to test against
	 * @return If an upvalue is allocated for this variable
	 */
	private boolean testIsAllocUpvalue(VarInfo var) {
		if (var.pc < 0) {
			return true;
		}
		BasicBlock block = pi.blocks[var.pc];
		if (var.pc > block.pc0) {
			return pi.vars[slot][var.pc - 1].upvalue != this;
		}
		if (block.prev == null) {
			var = pi.params[slot];
			if (var != null && var.upvalue != this) {
				return true;
			}
		} else {
			for (int i = 0, n = block.prev.length; i < n; i++) {
				var = pi.vars[slot][block.prev[i].pc1];
				if (var != null && var.upvalue != this) {
					return true;
				}
			}
		}
		return false;
	}

}
