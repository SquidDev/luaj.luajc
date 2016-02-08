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
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.squiddev.luaj.luajc.analysis.block.BasicBlock;
import org.squiddev.luaj.luajc.analysis.type.BasicType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Builds prototype info
 */
public final class AnalysisBuilder {
	private final ProtoInfo info;
	private final UpvalueInfo[][] openUpvalues;

	public AnalysisBuilder(ProtoInfo info) {
		this.info = info;
		openUpvalues = new UpvalueInfo[info.prototype.maxstacksize][];
	}

	/**
	 * Find all variables, storing if they are referenced and creating phi nodes,
	 * simplifying them if possible
	 *
	 * @return All phi nodes
	 */
	public List<PhiInfo> findVariables() {
		/**
		 * List of phi variables used
		 */
		ArrayList<PhiInfo> phis = new ArrayList<PhiInfo>();

		// Create storage for variables
		int nStack = info.prototype.maxstacksize;
		VarInfo[][] vars = info.vars;
		for (int i = 0; i < vars.length; i++) {
			vars[i] = new VarInfo[nStack];
		}

		int[] code = info.prototype.code;
		LuaValue[] constants = info.prototype.k;

		// Process instructions
		for (BasicBlock b0 : info.blockList) {
			// input from previous blocks
			VarInfo[] blockVars = b0.entry;
			int nPrevious = b0.prev != null ? b0.prev.length : 0;
			for (int slot = 0; slot < nStack; slot++) {
				VarInfo var = null;
				if (nPrevious == 0) {
					var = info.params[slot];
				} else if (nPrevious == 1) {
					var = vars[b0.prev[0].pc1][slot];
				} else {
					for (int i = 0; i < nPrevious; i++) {
						if (vars[b0.prev[i].pc1][slot] == VarInfo.INVALID) {
							var = VarInfo.INVALID;
							break;
						}
					}
				}
				if (var == null) {
					PhiInfo phi = new PhiInfo(info, slot, b0.pc0);
					phis.add(phi);
					var = phi;
				}
				blockVars[slot] = var;
			}

			System.arraycopy(blockVars, 0, vars[b0.pc0], 0, nStack);

			// Process instructions for this basic block
			for (int pc = b0.pc0; pc <= b0.pc1; pc++) {
				// Propagate previous values except at block boundaries
				if (pc > b0.pc0) propagateVars(pc - 1, pc);

				int ins = code[pc];
				int op = Lua.GET_OPCODE(ins);

				VarInfo[] pcVar = vars[pc];

				// Account for assignments, references and invalidation
				switch (op) {
					case Lua.OP_LOADK:     // A Bx    R(A) := Kst(Bx)
					{
						int a = Lua.GETARG_A(ins);
						pcVar[a] = new VarInfo(a, pc, BasicType.fromValue(constants[Lua.GETARG_Bx(ins)]));
						break;
					}
					case Lua.OP_LOADBOOL:  // A B  C  R(A) := (Bool)B; if (C) pc++
					{
						int a = Lua.GETARG_A(ins);
						pcVar[a] = new VarInfo(a, pc, BasicType.BOOLEAN);
						break;
					}
					case Lua.OP_NEWTABLE:  // A B  C  R(A) := {} (size = B,C)
					{
						int a = Lua.GETARG_A(ins);
						pcVar[a] = new VarInfo(a, pc, BasicType.VALUE);
						break;
					}
					case Lua.OP_GETUPVAL:  // A B     R(A) := UpValue[B]
					case Lua.OP_GETGLOBAL: // A Bx    R(A) := Gbl[Kst(Bx)]
					{
						int a = Lua.GETARG_A(ins);
						pcVar[a] = new VarInfo(a, pc);
						break;
					}

					case Lua.OP_MOVE:    // A B R(A) := R(B)
					case Lua.OP_UNM:     // A B R(A) := -R(B)
					case Lua.OP_NOT:     // A B R(A) := not R(B)
					case Lua.OP_LEN:     // A B R(A) := length of R(B)
					case Lua.OP_TESTSET: // A B C if (R(B) <=> C) then R(A) := R(B) else pc++
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						pcVar[b].reference(pc);
						pcVar[a] = new VarInfo(a, pc);
						break;
					}

					case Lua.OP_ADD: // A B C R(A) := RK(B) + RK(C)
					case Lua.OP_SUB: // A B C R(A) := RK(B) - RK(C)
					case Lua.OP_MUL: // A B C R(A) := RK(B) * RK(C)
					case Lua.OP_DIV: // A B C R(A) := RK(B) / RK(C)
					case Lua.OP_MOD: // A B C R(A) := RK(B) % RK(C)
					case Lua.OP_POW: // A B C R(A) := RK(B) ^ RK(C)
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						int c = Lua.GETARG_C(ins);
						if (!Lua.ISK(b)) pcVar[b].reference(pc);
						if (!Lua.ISK(c) && c != b) pcVar[c].reference(pc);
						pcVar[a] = new VarInfo(a, pc);
						break;
					}

					case Lua.OP_SETTABLE: // A B C R(A)[RK(B)]:= RK(C)
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						int c = Lua.GETARG_C(ins);
						pcVar[a].reference(pc);
						if (a != b && !Lua.ISK(b)) pcVar[b].reference(pc);
						if (a != c && b != c && !Lua.ISK(c)) pcVar[c].reference(pc);
						break;
					}

					case Lua.OP_CONCAT: // A B C R(A) := R(B) .. ... .. R(C)
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						int c = Lua.GETARG_C(ins);
						for (; b <= c; b++) {
							pcVar[b].reference(pc);
						}
						pcVar[a] = new VarInfo(a, pc);
						break;
					}

					case Lua.OP_FORPREP: // A sBx R(A)-=R(A+2); pc+=sBx
					{
						int a = Lua.GETARG_A(ins);
						pcVar[a + 2].reference(pc);
						pcVar[a].reference(pc);

						// The VM sets this, so it must be a number
						pcVar[a] = new VarInfo(a, pc, BasicType.NUMBER);
						break;
					}

					case Lua.OP_GETTABLE: // A B C R(A) := R(B)[RK(C)]
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						int c = Lua.GETARG_C(ins);
						pcVar[b].reference(pc);
						if (b != c && !Lua.ISK(c)) pcVar[c].reference(pc);
						pcVar[a] = new VarInfo(a, pc);
						break;
					}

					case Lua.OP_SELF: // A B C R(A+1) := R(B); R(A) := R(B)[RK(C)]
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						int c = Lua.GETARG_C(ins);
						pcVar[b].reference(pc);
						if (b != c && !Lua.ISK(c)) pcVar[c].reference(pc);
						pcVar[a] = new VarInfo(a, pc);
						pcVar[a + 1] = new VarInfo(a + 1, pc);
						break;
					}

					case Lua.OP_FORLOOP: // A sBx R(A)+=R(A+2); if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }
					{
						int a = Lua.GETARG_A(ins);
						pcVar[a].reference(pc);
						pcVar[a + 2].reference(pc);
						pcVar[a + 1].reference(pc);

						pcVar[a] = new VarInfo(a, pc, BasicType.NUMBER);
						pcVar[a].reference(pc);

						pcVar[a + 3] = new VarInfo(a + 3, pc, BasicType.NUMBER);
						break;
					}

					case Lua.OP_LOADNIL: // A B R(A) ... R(B) := nil
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						for (; a <= b; a++) {
							pcVar[a] = new VarInfo(a, pc, BasicType.VALUE);
						}
						break;
					}

					case Lua.OP_VARARG: // A B R(A), R(A+1), ..., R(A+B-1) = vararg
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						for (int j = 1; j < b; j++, a++) {
							pcVar[a] = new VarInfo(a, pc);
						}
						if (b == 0) {
							for (; a < nStack; a++) {
								pcVar[a] = VarInfo.INVALID;
							}
						}
						break;
					}

					case Lua.OP_CALL: // A B C R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						int c = Lua.GETARG_C(ins);
						pcVar[a].reference(pc);

						int max = b == 0 ? info.prototype.maxstacksize : a + b;
						for (int i = a + 1; i < max; i++) {
							VarInfo info = pcVar[i];
							if (info == VarInfo.INVALID) break;
							info.reference(pc);
						}
						for (int j = 0; j <= c - 2; j++, a++) {
							pcVar[a] = new VarInfo(a, pc);
						}
						for (; a < nStack; a++) {
							pcVar[a] = VarInfo.INVALID;
						}
						break;
					}

					case Lua.OP_TAILCALL: // A B C return R(A)(R(A+1), ... ,R(A+B-1))
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						pcVar[a].reference(pc);

						int max = b == 0 ? info.prototype.maxstacksize : a + b;
						for (int i = a + 1; i < max; i++) {
							VarInfo info = pcVar[i];
							if (info == VarInfo.INVALID) break;
							info.reference(pc);
						}
						break;
					}

					case Lua.OP_RETURN: // A B return R(A), ... ,R(A+B-2)
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						int max = b == 0 ? info.prototype.maxstacksize : a + b - 1;
						for (int i = a; i < max; i++) {
							VarInfo info = pcVar[i];
							if (info == VarInfo.INVALID) break;
							info.reference(pc);
						}
						break;
					}

					case Lua.OP_TFORLOOP: // A C R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2)); if R(A+3) ~= nil then R(A+2)=R(A+3) else pc++
					{
						int a = Lua.GETARG_A(ins);
						int c = Lua.GETARG_C(ins);
						pcVar[a++].reference(pc);
						pcVar[a++].reference(pc);
						pcVar[a++].reference(pc);
						for (int j = 0; j < c; j++, a++) {
							pcVar[a] = new VarInfo(a, pc);
						}
						for (; a < nStack; a++) {
							pcVar[a] = VarInfo.INVALID;
						}
						break;
					}

					case Lua.OP_CLOSURE: // A Bx R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n))
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_Bx(ins);
						int nups = info.prototype.p[b].nups;
						for (int k = 1; k <= nups; ++k) {
							int i = info.prototype.code[pc + k];
							if ((i & 4) == 0) {
								b = Lua.GETARG_B(i);
								pcVar[b].reference(pc);
							}
						}
						pcVar[a] = new VarInfo(a, pc);
						for (int k = 1; k <= nups; k++) {
							propagateVars(pc, pc + k);
						}
						pc += nups;
						break;
					}
					case Lua.OP_CLOSE: // A close all variables in the stack up to (>=) R(A)
					{
						int a = Lua.GETARG_A(ins);
						for (; a < nStack; a++) {
							pcVar[a] = VarInfo.INVALID;
						}
						break;
					}

					case Lua.OP_SETLIST: // A B C R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
					{
						int a = Lua.GETARG_A(ins);
						int b = Lua.GETARG_B(ins);
						pcVar[a].reference(pc);

						int max = b == 0 ? info.prototype.maxstacksize : a + b + 1;
						for (int i = a + 1; i < max; i++) {
							VarInfo info = pcVar[i];
							if (info == VarInfo.INVALID) break;
							info.reference(pc);
						}
						break;
					}

					case Lua.OP_SETGLOBAL: // A Bx Gbl[Kst(Bx)]:= R(A)
					case Lua.OP_SETUPVAL:  // A B  UpValue[B]:= R(A)
					case Lua.OP_TEST:      // A C  if not (R(A) <=> C) then pc++
					{
						int a = Lua.GETARG_A(ins);
						pcVar[a].reference(pc);
						break;
					}
					case Lua.OP_EQ: // A B C if ((RK(B) == RK(C)) ~= A) then pc++
					case Lua.OP_LT: // A B C if ((RK(B) <  RK(C)) ~= A) then pc++
					case Lua.OP_LE: // A B C if ((RK(B) <= RK(C)) ~= A) then pc++
					{
						int b = Lua.GETARG_B(ins);
						int c = Lua.GETARG_C(ins);
						if (!Lua.ISK(b)) pcVar[b].reference(pc);
						if (!Lua.ISK(c)) pcVar[c].reference(pc);
						break;
					}

					case Lua.OP_JMP: // sBx pc+=sBx
						break;

					default:
						throw new IllegalStateException("unhandled opcode: " + ins);
				}
			}
		}

		return replaceTrivialPhiVariables(phis);
	}

	/**
	 * Replace phi variables that reference the same thing
	 *
	 * @param phis List of phi nodes to replace
	 * @return All phi nodes that haven't been replaced
	 */
	private List<PhiInfo> replaceTrivialPhiVariables(List<PhiInfo> phis) {
		// Replace trivial Phi variables
		Iterator<PhiInfo> phiIterator = phis.iterator();
		while (phiIterator.hasNext()) {
			PhiInfo phi = phiIterator.next();

			// We only need to assign this if it isn't replacable
			VarInfo newVar = phi.resolvePhiVariableValues();
			if (newVar == null) {
				int pc = info.blocks[phi.pc].pc0;

				List<VarInfo> localPhis = info.phiPositions[pc];
				if (localPhis == null) {
					localPhis = new ArrayList<VarInfo>();
					info.phiPositions[pc] = localPhis;
				}

				localPhis.add(phi);
			} else {
				substituteVariable(phi.slot, phi, newVar);
				phiIterator.remove();
			}
		}

		return Collections.unmodifiableList(phis);
	}

	/**
	 * Replace a variable in a specific slot
	 *
	 * @param slot   The slot to replace at
	 * @param oldVar The old variable
	 * @param newVar The new variable
	 */
	private void substituteVariable(int slot, VarInfo oldVar, VarInfo newVar) {
		VarInfo[][] vars = info.vars;
		BasicBlock[] blocks = info.blocks;
		int length = info.prototype.code.length;
		for (int pc = 0; pc < length; pc++) {
			BasicBlock block = blocks[pc];
			if (block.pc0 == pc && block.entry[slot] == oldVar) {
				block.entry[slot] = newVar;
			}
			if (vars[pc][slot] == oldVar) {
				vars[pc][slot] = newVar;
			}
		}
	}

	/**
	 * Copy variables from one PC to another
	 *
	 * @param pcFrom The old PC to copy from
	 * @param pcTo   The new PC to copy to
	 */
	private void propagateVars(int pcFrom, int pcTo) {
		VarInfo[][] vars = info.vars;
		VarInfo[] to = vars[pcTo], from = vars[pcFrom];
		System.arraycopy(from, 0, to, 0, to.length);
	}

	/**
	 * Fill all arguments
	 */
	public void fillArguments() {
		int max = info.prototype.maxstacksize;
		for (int slot = 0; slot < max; slot++) {
			VarInfo v = VarInfo.param(slot);
			info.params[slot] = v;
		}
	}

	/**
	 * Find upvalues and create child prototypes
	 */
	public void findUpvalues() {
		int[] code = info.prototype.code;
		int n = code.length;

		// Propagate to inner prototypes
		for (int pc = 0; pc < n; pc++) {
			if (Lua.GET_OPCODE(code[pc]) == Lua.OP_CLOSURE) {
				int bx = Lua.GETARG_Bx(code[pc]);
				Prototype childPrototype = info.prototype.p[bx];
				String childName = info.name + "$" + bx;

				UpvalueInfo[] childUpvalues = null;

				if (childPrototype.nups > 0) {
					childUpvalues = new UpvalueInfo[childPrototype.nups];
					for (int j = 0; j < childPrototype.nups; ++j) {
						int i = code[++pc];
						int b = Lua.GETARG_B(i);
						childUpvalues[j] = (i & 4) != 0 ? info.upvalues[b] : findOpenUp(pc, b);
					}
				}

				info.subprotos[bx] = new ProtoInfo(childPrototype, info.loader, childName, childUpvalues);
			}
		}

		// Mark all upvalues that are written locally as read/write
		for (int instruction : code) {
			if (Lua.GET_OPCODE(instruction) == Lua.OP_SETUPVAL) {
				info.upvalues[Lua.GETARG_B(instruction)].readWrite = true;
			}
		}
	}

	private UpvalueInfo findOpenUp(int pc, int slot) {
		if (openUpvalues[slot] == null) {
			openUpvalues[slot] = new UpvalueInfo[info.prototype.code.length];
		}
		if (openUpvalues[slot][pc] != null) {
			return openUpvalues[slot][pc];
		}
		UpvalueInfo u = new UpvalueInfo(info, pc, slot);
		for (int i = 0, n = info.prototype.code.length; i < n; ++i) {
			VarInfo thisInfo = info.vars[i][slot];
			if (thisInfo != null && thisInfo.upvalue == u) {
				openUpvalues[slot][i] = u;
			}
		}
		return u;
	}
}
