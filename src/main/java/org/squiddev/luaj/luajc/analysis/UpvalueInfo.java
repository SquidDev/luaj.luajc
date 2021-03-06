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
package org.squiddev.luaj.luajc.analysis;

import org.luaj.vm2.Lua;
import org.squiddev.luaj.luajc.analysis.block.BasicBlock;

import java.util.ArrayList;
import java.util.List;

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
	 * List of variables involved
	 */
	private final List<VarInfo> vars = new ArrayList<VarInfo>();

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

		includeVarAndPosteriorVars(pi.vars[pc][slot]);
		for (VarInfo var : vars) {
			var.allocUpvalue = testIsAllocUpvalue(var);
		}
		readWrite = vars.size() > 1;
	}

	/**
	 * Find variables that this upvalue links to
	 *
	 * @param var The variable to use
	 * @return If we have visited this before, hence a loop has been found
	 */
	private boolean includeVarAndPosteriorVars(VarInfo var) {
		// We can't do anything with this
		if (var == null || var == VarInfo.INVALID) return false;

		// We've already set this, so we must be in a loop
		if (var.upvalue == this) return true;

		var.upvalue = this;
		vars.add(var);

		// If this is a loop, then we can ignore this
		if (isLoopVariable(var)) {
			includePriorVarsIgnoreLoops(var);
			return false;
		}

		// Scan recursively
		boolean loopDetected = includePosteriorVarsCheckLoops(var);
		if (loopDetected) includePriorVarsIgnoreLoops(var);

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

	/**
	 * Include all changes
	 *
	 * @param prior The instruction to check
	 * @return If a loop was detected.
	 */
	private boolean includePosteriorVarsCheckLoops(VarInfo prior) {
		boolean loopDetected = false;
		for (BasicBlock b : pi.blockList) {
			// Get the variable definition at the last instruction in this block
			VarInfo var = pi.vars[b.pc1][slot];

			if (var == prior) {
				// If it hasn't changed then scan next instructions
				if (b.next != null) {
					for (BasicBlock b1 : b.next) {
						VarInfo v1 = b1.entry[slot];
						if (v1 != prior) {
							loopDetected |= includeVarAndPosteriorVars(v1);
							if (v1.isPhiVar()) {
								includePriorVarsIgnoreLoops(v1);
							}
						}
					}
				}
			} else {
				// Otherwise it may have changed within this block. Scan in reverse to find the correct instruction.
				boolean hit = false;
				for (int pc = b.pc1 - 1; pc >= b.pc0; pc--) {
					// This is the same, so the next instruction must be a modification
					if (pi.vars[pc][slot] == prior) {
						loopDetected |= includeVarAndPosteriorVars(pi.vars[pc + 1][slot]);
						hit = true;
						break;
					}
				}

				if (!hit) {
					if (b.entry[slot] == prior) {
						loopDetected |= includeVarAndPosteriorVars(pi.vars[b.pc0][slot]);
					}
				}
			}
		}
		return loopDetected;
	}

	private void includePriorVarsIgnoreLoops(VarInfo poster) {
		for (BasicBlock b : pi.blockList) {
			// Get the variable definition at the first instruction in this block
			VarInfo var = b.entry[slot];

			// If it hasn't changed
			if (var == poster) {
				if (b.prev != null) {
					for (BasicBlock b0 : b.prev) {
						VarInfo v0 = b0.entry[slot];
						if (v0 != poster) {
							includeVarAndPosteriorVars(v0);
						}
					}
				}
			} else {
				if (pi.vars[b.pc0][slot] == poster) {
					includeVarAndPosteriorVars(var);
				} else {
					for (int pc = b.pc0 + 1; pc <= b.pc1; pc++) {
						if (pi.vars[pc][slot] == poster) {
							includeVarAndPosteriorVars(pi.vars[pc - 1][slot]);
							break;
						}
					}
				}
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(pi.name);
		for (int i = 0; i < vars.size(); i++) {
			sb.append(i > 0 ? "," : " ");
			sb.append(vars.get(i));
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
			return pi.vars[var.pc - 1][slot].upvalue != this;
		}
		if (block.prev == null) {
			var = pi.params[slot];
			if (var != null && var.upvalue != this) {
				return true;
			}
		} else {
			for (int i = 0, n = block.prev.length; i < n; i++) {
				var = pi.vars[block.prev[i].pc1][slot];
				if (var != null && var.upvalue != this) {
					return true;
				}
			}
		}
		return false;
	}

}
