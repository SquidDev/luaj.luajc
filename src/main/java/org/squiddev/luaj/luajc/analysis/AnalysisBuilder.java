package org.squiddev.luaj.luajc.analysis;

import org.luaj.vm2.Lua;
import org.luaj.vm2.Prototype;

import java.util.HashSet;
import java.util.Set;

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
	 */
	public void findVariables() {
		/**
		 * List of phi variables used
		 */
		Set<VarInfo> phis = new HashSet<VarInfo>();

		// Create storage for variables
		int nStack = info.prototype.maxstacksize;
		VarInfo[][] vars = info.vars;
		for (int i = 0; i < vars.length; i++) {
			vars[i] = new VarInfo[nStack];
		}

		// Process instructions
		for (BasicBlock b0 : info.blockList) {
			// input from previous blocks
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
					var = VarInfo.phi(info, slot, b0.pc0);
					phis.add(var);
				}
				vars[b0.pc0][slot] = var;
			}

			// Process instructions for this basic block
			for (int pc = b0.pc0; pc <= b0.pc1; pc++) {
				// Propagate previous values except at block boundaries
				if (pc > b0.pc0) propagateVars(pc - 1, pc);

				int a, b, c, nups;
				int ins = info.prototype.code[pc];
				int op = Lua.GET_OPCODE(ins);

				VarInfo[] pcVar = vars[pc];

				// Account for assignments, references and invalidation
				switch (op) {
					case Lua.OP_LOADK:     // A Bx    R(A) := Kst(Bx)
					case Lua.OP_LOADBOOL:  // A B  C  R(A) := (Bool)B; if (C) pc++
					case Lua.OP_GETUPVAL:  // A B     R(A) := UpValue[B]
					case Lua.OP_GETGLOBAL: // A Bx    R(A) := Gbl[Kst(Bx)]
					case Lua.OP_NEWTABLE:  // A B  C  R(A) := {} (size = B,C)
						a = Lua.GETARG_A(ins);
						pcVar[a] = new VarInfo(a, pc);
						break;

					case Lua.OP_MOVE:    // A B R(A) := R(B)
					case Lua.OP_UNM:     // A B R(A) := -R(B)
					case Lua.OP_NOT:     // A B R(A) := not R(B)
					case Lua.OP_LEN:     // A B R(A) := length of R(B)
					case Lua.OP_TESTSET: // A B C if (R(B) <=> C) then R(A) := R(B) else pc++
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						pcVar[b].isReferenced = true;
						pcVar[a] = new VarInfo(a, pc);
						break;

					case Lua.OP_ADD: // A B C R(A) := RK(B) + RK(C)
					case Lua.OP_SUB: // A B C R(A) := RK(B) - RK(C)
					case Lua.OP_MUL: // A B C R(A) := RK(B) * RK(C)
					case Lua.OP_DIV: // A B C R(A) := RK(B) / RK(C)
					case Lua.OP_MOD: // A B C R(A) := RK(B) % RK(C)
					case Lua.OP_POW: // A B C R(A) := RK(B) ^ RK(C)
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						if (!Lua.ISK(b)) pcVar[b].isReferenced = true;
						if (!Lua.ISK(c)) pcVar[c].isReferenced = true;
						pcVar[a] = new VarInfo(a, pc);
						break;

					case Lua.OP_SETTABLE: // A B C R(A)[RK(B)]:= RK(C)
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						pcVar[a].isReferenced = true;
						if (!Lua.ISK(b)) pcVar[b].isReferenced = true;
						if (!Lua.ISK(c)) pcVar[c].isReferenced = true;
						break;

					case Lua.OP_CONCAT: // A B C R(A) := R(B) .. ... .. R(C)
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						for (; b <= c; b++) {
							pcVar[b].isReferenced = true;
						}
						pcVar[a] = new VarInfo(a, pc);
						break;

					case Lua.OP_FORPREP: // A sBx R(A)-=R(A+2); pc+=sBx
						a = Lua.GETARG_A(ins);
						pcVar[a + 2].isReferenced = true;
						pcVar[a] = new VarInfo(a, pc);
						break;

					case Lua.OP_GETTABLE: // A B C R(A) := R(B)[RK(C)]
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						pcVar[b].isReferenced = true;
						if (!Lua.ISK(c)) pcVar[c].isReferenced = true;
						pcVar[a] = new VarInfo(a, pc);
						break;

					case Lua.OP_SELF: // A B C R(A+1) := R(B); R(A) := R(B)[RK(C)]
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						pcVar[b].isReferenced = true;
						if (!Lua.ISK(c)) pcVar[c].isReferenced = true;
						pcVar[a] = new VarInfo(a, pc);
						pcVar[a + 1] = new VarInfo(a + 1, pc);
						break;

					case Lua.OP_FORLOOP: // A sBx R(A)+=R(A+2); if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }
						a = Lua.GETARG_A(ins);
						pcVar[a].isReferenced = true;
						pcVar[a + 2].isReferenced = true;
						pcVar[a] = new VarInfo(a, pc);
						pcVar[a].isReferenced = true;
						pcVar[a + 1].isReferenced = true;
						pcVar[a + 3] = new VarInfo(a + 3, pc);
						break;

					case Lua.OP_LOADNIL: // A B R(A) ... R(B) := nil
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						for (; a <= b; a++) {
							pcVar[a] = new VarInfo(a, pc);
						}
						break;

					case Lua.OP_VARARG: // A B R(A), R(A+1), ..., R(A+B-1) = vararg
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						for (int j = 1; j < b; j++, a++) {
							pcVar[a] = new VarInfo(a, pc);
						}
						if (b == 0) {
							for (; a < nStack; a++) {
								pcVar[a] = VarInfo.INVALID;
							}
						}
						break;

					case Lua.OP_CALL: // A B C R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						pcVar[a].isReferenced = true;
						pcVar[a].isReferenced = true;
						for (int i = 1; i <= b - 1; i++) {
							pcVar[a + i].isReferenced = true;
						}
						for (int j = 0; j <= c - 2; j++, a++) {
							pcVar[a] = new VarInfo(a, pc);
						}
						for (; a < nStack; a++) {
							pcVar[a] = VarInfo.INVALID;
						}
						break;

					case Lua.OP_TAILCALL: // A B C return R(A)(R(A+1), ... ,R(A+B-1))
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						pcVar[a].isReferenced = true;
						for (int i = 1; i <= b - 1; i++) {
							pcVar[a + i].isReferenced = true;
						}
						break;

					case Lua.OP_RETURN: // A B return R(A), ... ,R(A+B-2)
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						for (int i = 0; i <= b - 2; i++) {
							pcVar[a + i].isReferenced = true;
						}
						break;

					case Lua.OP_TFORLOOP: // A C R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2)); if R(A+3) ~= nil then R(A+2)=R(A+3) else pc++
						a = Lua.GETARG_A(ins);
						c = Lua.GETARG_C(ins);
						pcVar[a++].isReferenced = true;
						pcVar[a++].isReferenced = true;
						pcVar[a++].isReferenced = true;
						for (int j = 0; j < c; j++, a++) {
							pcVar[a] = new VarInfo(a, pc);
						}
						for (; a < nStack; a++) {
							pcVar[a] = VarInfo.INVALID;
						}
						break;

					case Lua.OP_CLOSURE: // A Bx R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n))
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_Bx(ins);
						nups = info.prototype.p[b].nups;
						for (int k = 1; k <= nups; ++k) {
							int i = info.prototype.code[pc + k];
							if ((i & 4) == 0) {
								b = Lua.GETARG_B(i);
								pcVar[b].isReferenced = true;
							}
						}
						pcVar[a] = new VarInfo(a, pc);
						for (int k = 1; k <= nups; k++) {
							propagateVars(pc, pc + k);
						}
						pc += nups;
						break;
					case Lua.OP_CLOSE: // A close all variables in the stack up to (>=) R(A)
						a = Lua.GETARG_A(ins);
						for (; a < nStack; a++) {
							pcVar[a] = VarInfo.INVALID;
						}
						break;

					case Lua.OP_SETLIST: // A B C R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						pcVar[a].isReferenced = true;
						for (int i = 1; i <= b; i++) {
							pcVar[a + i].isReferenced = true;
						}
						break;

					case Lua.OP_SETGLOBAL: // A Bx Gbl[Kst(Bx)]:= R(A)
					case Lua.OP_SETUPVAL:  // A B  UpValue[B]:= R(A)
					case Lua.OP_TEST:      // A C  if not (R(A) <=> C) then pc++
						a = Lua.GETARG_A(ins);
						pcVar[a].isReferenced = true;
						break;

					case Lua.OP_EQ: // A B C if ((RK(B) == RK(C)) ~= A) then pc++
					case Lua.OP_LT: // A B C if ((RK(B) <  RK(C)) ~= A) then pc++
					case Lua.OP_LE: // A B C if ((RK(B) <= RK(C)) ~= A) then pc++
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						if (!Lua.ISK(b)) pcVar[b].isReferenced = true;
						if (!Lua.ISK(c)) pcVar[c].isReferenced = true;
						break;

					case Lua.OP_JMP: // sBx pc+=sBx
						break;

					default:
						throw new IllegalStateException("unhandled opcode: " + ins);
				}
			}
		}

		replaceTrivialPhiVariables(phis);
	}

	/**
	 * Replace phi variables that reference the same thing
	 *
	 * @param phis List of phi nodes to replace
	 */
	private void replaceTrivialPhiVariables(Set<VarInfo> phis) {
		// Replace trivial Phi variables
		for (BasicBlock b0 : info.blockList) {
			VarInfo[] vars = info.vars[b0.pc0];
			for (int slot = 0; slot < info.prototype.maxstacksize; slot++) {
				VarInfo oldVar = vars[slot];
				VarInfo newVar = oldVar.resolvePhiVariableValues();
				if (newVar != null) {
					substituteVariable(slot, oldVar, newVar);
				}

				phis.remove(oldVar);
			}
		}

		// Some phi variables are overwritten resulting in slots not being assigned
		// https://github.com/SquidDev-CC/Studio/pull/13
		for (VarInfo phi : phis) {
			phi.resolvePhiVariableValues();
		}
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
		int length = info.prototype.code.length;
		for (int pc = 0; pc < length; pc++) {
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
