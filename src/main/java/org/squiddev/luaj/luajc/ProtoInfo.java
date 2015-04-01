package org.squiddev.luaj.luajc;

import org.luaj.vm2.Lua;
import org.luaj.vm2.Print;
import org.luaj.vm2.Prototype;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Prototype information for static single-assignment analysis
 */
public class ProtoInfo {
	/**
	 * The name of the prototype
	 */
	public final String name;

	/**
	 * The prototype that this info is about
	 */
	public final Prototype prototype;
	/**
	 * List of child prototypes or null
	 */
	public final ProtoInfo[] subprotos;

	/**
	 * List of blocks for analysis of code branching
	 */
	public final BasicBlock[] blocks;

	/**
	 * Blocks in breadth-first order
	 */
	public final BasicBlock[] blockList;

	/**
	 * Parameters and initial values of stack variables
	 */
	public final VarInfo[] params;

	/**
	 * Variables in the form vars[pc][slot].
	 */
	public final VarInfo[][] vars;

	/**
	 * List of upvalues from outer scope
	 */
	public final UpvalueInfo[] upvalues;

	/**
	 * Upvalues allocated by this prototype
	 */
	public final UpvalueInfo[][] openUpvalues;

	/**
	 * Storage for all phi variables in the prototype
	 */
	private final Set<VarInfo> phis = new HashSet<>();

	public ProtoInfo(Prototype p, String name) {
		this(p, name, null);
	}

	private ProtoInfo(Prototype p, String name, UpvalueInfo[] u) {
		this.name = name;
		prototype = p;
		upvalues = u;
		subprotos = p.p != null && p.p.length > 0 ? new ProtoInfo[p.p.length] : null;

		// find basic blocks
		blocks = BasicBlock.findBasicBlocks(p);
		blockList = BasicBlock.findLiveBlocks(blocks);

		// params are inputs to first block
		params = new VarInfo[p.maxstacksize];
		for (int slot = 0; slot < p.maxstacksize; slot++) {
			VarInfo v = VarInfo.PARAM(slot);
			params[slot] = v;
		}

		// find variables
		vars = findVariables();
		replaceTrivialPhiVariables();

		// find upvalues, create sub-prototypes
		openUpvalues = new UpvalueInfo[p.maxstacksize][];
		findUpvalues();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		// prototpye name
		sb.append("proto '").append(name).append("'\n");

		// upvalues from outer scopes
		for (int i = 0, n = (upvalues != null ? upvalues.length : 0); i < n; i++) {
			sb.append("\tup[").append(i).append("]: ").append(upvalues[i]).append("\n");
		}

		// basic blocks
		for (BasicBlock b : blockList) {
			int pc0 = b.pc0;
			sb.append("\tblock ").append(b.toString());
			appendOpenUps(sb, -1);

			// instructions
			for (int pc = pc0; pc <= b.pc1; pc++) {

				// open upvalue storage
				appendOpenUps(sb, pc);

				// opcode
				sb.append("\t\t");
				for (int j = 0; j < prototype.maxstacksize; j++) {
					VarInfo v = vars[j][pc];
					String u = (v == null ? "" : v.upvalue != null ? !v.upvalue.readWrite ? "[C] " : (v.allocUpvalue && v.pc == pc ? "[*] " : "[]  ") : "    ");
					String s = v == null ? "null   " : String.valueOf(v);
					sb.append(s).append(u);
				}
				sb.append("  ");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream ops = Print.ps;
				Print.ps = new PrintStream(baos);
				try {
					Print.printOpCode(prototype, pc);
				} finally {
					Print.ps.close();
					Print.ps = ops;
				}
				sb.append(baos.toString());
				sb.append("\n");
			}
		}

		// nested functions
		for (int i = 0, n = subprotos != null ? subprotos.length : 0; i < n; i++) {
			sb.append(subprotos[i].toString());
		}

		return sb.toString();
	}

	private void appendOpenUps(StringBuffer sb, int pc) {
		for (int j = 0; j < prototype.maxstacksize; j++) {
			VarInfo v = (pc < 0 ? params[j] : vars[j][pc]);
			if (v != null && v.pc == pc && v.allocUpvalue) {
				sb.append("\t\topen: ").append(v.upvalue).append("\n");
			}
		}
	}

	/**
	 * Find variables and resolve Phi variables
	 *
	 * @return The variable list
	 */
	private VarInfo[][] findVariables() {
		/**
		 * List of phi variables used
		 */
		Set<VarInfo> phis = this.phis;

		// Create storage for variables
		int n = prototype.code.length;
		int m = prototype.maxstacksize;
		VarInfo[][] v = new VarInfo[m][];
		for (int i = 0; i < v.length; i++) {
			v[i] = new VarInfo[n];
		}

		// Process instructions
		for (BasicBlock b0 : blockList) {
			// input from previous blocks
			int nPrevious = b0.prev != null ? b0.prev.length : 0;
			for (int slot = 0; slot < m; slot++) {
				VarInfo var = null;
				if (nPrevious == 0) {
					var = params[slot];
				} else if (nPrevious == 1) {
					var = v[slot][b0.prev[0].pc1];
				} else {
					for (int i = 0; i < nPrevious; i++) {
						if (v[slot][b0.prev[i].pc1] == VarInfo.INVALID) {
							var = VarInfo.INVALID;
							break;
						}
					}
				}
				if (var == null) {
					var = VarInfo.PHI(this, slot, b0.pc0);
					phis.add(var);
				}
				v[slot][b0.pc0] = var;
			}

			// Process instructions for this basic block
			for (int pc = b0.pc0; pc <= b0.pc1; pc++) {

				// Propagate previous values except at block boundaries
				if (pc > b0.pc0) {
					propagateVars(v, pc - 1, pc);
				}

				int a, b, c, nups;
				int ins = prototype.code[pc];
				int op = Lua.GET_OPCODE(ins);

				// Account for assignments, references and invalidation
				switch (op) {
					case Lua.OP_LOADK:/*	A Bx	R(A) := Kst(Bx)					*/
					case Lua.OP_LOADBOOL:/*	A B C	R(A) := (Bool)B; if (C) pc++			*/
					case Lua.OP_GETUPVAL: /*	A B	R(A) := UpValue[B]				*/
					case Lua.OP_GETGLOBAL: /*	A Bx	R(A) := Gbl[Kst(Bx)]				*/
					case Lua.OP_NEWTABLE: /*	A B C	R(A) := {} (size = B,C)				*/
						a = Lua.GETARG_A(ins);
						v[a][pc] = new VarInfo(a, pc);
						break;

					case Lua.OP_MOVE:/*	A B	R(A) := R(B)					*/
					case Lua.OP_UNM: /*	A B	R(A) := -R(B)					*/
					case Lua.OP_NOT: /*	A B	R(A) := not R(B)				*/
					case Lua.OP_LEN: /*	A B	R(A) := length of R(B)				*/
					case Lua.OP_TESTSET: /*	A B C	if (R(B) <=> C) then R(A) := R(B) else pc++	*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						v[b][pc].isReferenced = true;
						v[a][pc] = new VarInfo(a, pc);
						break;

					case Lua.OP_ADD: /*	A B C	R(A) := RK(B) + RK(C)				*/
					case Lua.OP_SUB: /*	A B C	R(A) := RK(B) - RK(C)				*/
					case Lua.OP_MUL: /*	A B C	R(A) := RK(B) * RK(C)				*/
					case Lua.OP_DIV: /*	A B C	R(A) := RK(B) / RK(C)				*/
					case Lua.OP_MOD: /*	A B C	R(A) := RK(B) % RK(C)				*/
					case Lua.OP_POW: /*	A B C	R(A) := RK(B) ^ RK(C)				*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						if (!Lua.ISK(b)) v[b][pc].isReferenced = true;
						if (!Lua.ISK(c)) v[c][pc].isReferenced = true;
						v[a][pc] = new VarInfo(a, pc);
						break;

					case Lua.OP_SETTABLE: /*	A B C	R(A)[RK(B)]:= RK(C)				*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						v[a][pc].isReferenced = true;
						if (!Lua.ISK(b)) v[b][pc].isReferenced = true;
						if (!Lua.ISK(c)) v[c][pc].isReferenced = true;
						break;

					case Lua.OP_CONCAT: /*	A B C	R(A) := R(B).. ... ..R(C)			*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						for (; b <= c; b++) {
							v[b][pc].isReferenced = true;
						}
						v[a][pc] = new VarInfo(a, pc);
						break;

					case Lua.OP_FORPREP: /*	A sBx	R(A)-=R(A+2); pc+=sBx				*/
						a = Lua.GETARG_A(ins);
						v[a + 2][pc].isReferenced = true;
						v[a][pc] = new VarInfo(a, pc);
						break;

					case Lua.OP_GETTABLE: /*	A B C	R(A) := R(B)[RK(C)]				*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						v[b][pc].isReferenced = true;
						if (!Lua.ISK(c)) v[c][pc].isReferenced = true;
						v[a][pc] = new VarInfo(a, pc);
						break;

					case Lua.OP_SELF: /*	A B C	R(A+1) := R(B); R(A) := R(B)[RK(C)]		*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						v[b][pc].isReferenced = true;
						if (!Lua.ISK(c)) v[c][pc].isReferenced = true;
						v[a][pc] = new VarInfo(a, pc);
						v[a + 1][pc] = new VarInfo(a + 1, pc);
						break;

					case Lua.OP_FORLOOP: /*	A sBx	R(A)+=R(A+2);
					if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }*/
						a = Lua.GETARG_A(ins);
						v[a][pc].isReferenced = true;
						v[a + 2][pc].isReferenced = true;
						v[a][pc] = new VarInfo(a, pc);
						v[a][pc].isReferenced = true;
						v[a + 1][pc].isReferenced = true;
						v[a + 3][pc] = new VarInfo(a + 3, pc);
						break;

					case Lua.OP_LOADNIL: /*	A B	R(A) := ... := R(B) := nil			*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						for (; a <= b; a++) {
							v[a][pc] = new VarInfo(a, pc);
						}
						break;

					case Lua.OP_VARARG: /*	A B	R(A), R(A+1), ..., R(A+B-1) = vararg		*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						for (int j = 1; j < b; j++, a++) {
							v[a][pc] = new VarInfo(a, pc);
						}
						if (b == 0) {
							for (; a < m; a++) {
								v[a][pc] = VarInfo.INVALID;
							}
						}
						break;

					case Lua.OP_CALL: /*	A B C	R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1)) */
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						v[a][pc].isReferenced = true;
						v[a][pc].isReferenced = true;
						for (int i = 1; i <= b - 1; i++) {
							v[a + i][pc].isReferenced = true;
						}
						for (int j = 0; j <= c - 2; j++, a++) {
							v[a][pc] = new VarInfo(a, pc);
						}
						for (; a < m; a++) {
							v[a][pc] = VarInfo.INVALID;
						}
						break;

					case Lua.OP_TAILCALL: /*	A B C	return R(A)(R(A+1), ... ,R(A+B-1))		*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						v[a][pc].isReferenced = true;
						for (int i = 1; i <= b - 1; i++) {
							v[a + i][pc].isReferenced = true;
						}
						break;

					case Lua.OP_RETURN: /*	A B	return R(A), ... ,R(A+B-2)	(see note)	*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						for (int i = 0; i <= b - 2; i++) {
							v[a + i][pc].isReferenced = true;
						}
						break;

					case Lua.OP_TFORLOOP: /*	A C	R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2));
					                    if R(A+3) ~= nil then R(A+2)=R(A+3) else pc++	*/
						a = Lua.GETARG_A(ins);
						c = Lua.GETARG_C(ins);
						v[a++][pc].isReferenced = true;
						v[a++][pc].isReferenced = true;
						v[a++][pc].isReferenced = true;
						for (int j = 0; j < c; j++, a++) {
							v[a][pc] = new VarInfo(a, pc);
						}
						for (; a < m; a++) {
							v[a][pc] = VarInfo.INVALID;
						}
						break;

					case Lua.OP_CLOSURE: /*	A Bx	R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n))	*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_Bx(ins);
						nups = prototype.p[b].nups;
						for (int k = 1; k <= nups; ++k) {
							int i = prototype.code[pc + k];
							if ((i & 4) == 0) {
								b = Lua.GETARG_B(i);
								v[b][pc].isReferenced = true;
							}
						}
						v[a][pc] = new VarInfo(a, pc);
						for (int k = 1; k <= nups; k++) {
							propagateVars(v, pc, pc + k);
						}
						pc += nups;
						break;
					case Lua.OP_CLOSE: /*	A 	close all variables in the stack up to (>=) R(A)*/
						a = Lua.GETARG_A(ins);
						for (; a < m; a++) {
							v[a][pc] = VarInfo.INVALID;
						}
						break;

					case Lua.OP_SETLIST: /*	A B C	R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B	*/
						a = Lua.GETARG_A(ins);
						b = Lua.GETARG_B(ins);
						v[a][pc].isReferenced = true;
						for (int i = 1; i <= b; i++) {
							v[a + i][pc].isReferenced = true;
						}
						break;

					case Lua.OP_SETGLOBAL: /*	A Bx	Gbl[Kst(Bx)]:= R(A)				*/
					case Lua.OP_SETUPVAL: /*	A B	UpValue[B]:= R(A)				*/
					case Lua.OP_TEST: /*	A C	if not (R(A) <=> C) then pc++			*/
						a = Lua.GETARG_A(ins);
						v[a][pc].isReferenced = true;
						break;

					case Lua.OP_EQ: /*	A B C	if ((RK(B) == RK(C)) ~= A) then pc++		*/
					case Lua.OP_LT: /*	A B C	if ((RK(B) <  RK(C)) ~= A) then pc++  		*/
					case Lua.OP_LE: /*	A B C	if ((RK(B) <= RK(C)) ~= A) then pc++  		*/
						b = Lua.GETARG_B(ins);
						c = Lua.GETARG_C(ins);
						if (!Lua.ISK(b)) v[b][pc].isReferenced = true;
						if (!Lua.ISK(c)) v[c][pc].isReferenced = true;
						break;

					case Lua.OP_JMP: /*	sBx	pc+=sBx					*/
						break;

					default:
						throw new IllegalStateException("unhandled opcode: " + ins);
				}
			}
		}


		return v;
	}

	/**
	 * Replace phi variables that reference the same thing
	 */
	private void replaceTrivialPhiVariables() {
		Set<VarInfo> phis = this.phis;
		// Replace trivial Phi variables
		for (BasicBlock b0 : blockList) {
			for (int slot = 0; slot < prototype.maxstacksize; slot++) {
				VarInfo oldVar = vars[slot][b0.pc0];
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
		VarInfo[] vars = this.vars[slot];
		int length = vars.length;
		for (int i = 0; i < length; i++) {
			if (vars[i] == oldVar) {
				vars[i] = newVar;
			}
		}
	}


	/**
	 * Find upvalues and create child prototypes
	 */
	private void findUpvalues() {
		int[] code = prototype.code;
		int n = code.length;

		// Propagate to inner prototypes
		for (int pc = 0; pc < n; pc++) {
			if (Lua.GET_OPCODE(code[pc]) == Lua.OP_CLOSURE) {
				int bx = Lua.GETARG_Bx(code[pc]);
				Prototype childPrototype = prototype.p[bx];
				String childName = name + "$" + bx;

				UpvalueInfo[] childUpvalues = childPrototype.nups > 0 ? new UpvalueInfo[childPrototype.nups] : null;

				for (int j = 0; j < childPrototype.nups; ++j) {
					int i = code[++pc];
					int b = Lua.GETARG_B(i);
					childUpvalues[j] = (i & 4) != 0 ? upvalues[b] : findOpenUp(pc, b);
				}

				subprotos[bx] = new ProtoInfo(childPrototype, childName, childUpvalues);
			}
		}

		// Mark all upvalues that are written locally as read/write
		for (int instruction : code) {
			if (Lua.GET_OPCODE(instruction) == Lua.OP_SETUPVAL) {
				upvalues[Lua.GETARG_B(instruction)].readWrite = true;
			}
		}
	}

	private UpvalueInfo findOpenUp(int pc, int slot) {
		if (openUpvalues[slot] == null) {
			openUpvalues[slot] = new UpvalueInfo[prototype.code.length];
		}
		if (openUpvalues[slot][pc] != null) {
			return openUpvalues[slot][pc];
		}
		UpvalueInfo u = new UpvalueInfo(this, pc, slot);
		for (int i = 0, n = prototype.code.length; i < n; ++i) {
			if (vars[slot][i] != null && vars[slot][i].upvalue == u) {
				openUpvalues[slot][i] = u;
			}
		}
		return u;
	}

	/**
	 * Check if this is an assignment to an upvalue
	 *
	 * @param pc   The current PC
	 * @param slot The slot the upvalue is stored in
	 * @return If an upvalue is assigned to at this point
	 */
	public boolean isUpvalueAssign(int pc, int slot) {
		VarInfo v = pc < 0 ? params[slot] : vars[slot][pc];
		return v != null && v.upvalue != null && v.upvalue.readWrite;
	}

	/**
	 * Check if this is the creation of an upvalue
	 *
	 * @param pc   The current PC
	 * @param slot The slot the upvalue is stored in
	 * @return If this is where the upvalue is created
	 */
	public boolean isUpvalueCreate(int pc, int slot) {
		VarInfo v = pc < 0 ? params[slot] : vars[slot][pc];
		return v != null && v.upvalue != null && v.upvalue.readWrite && v.allocUpvalue && pc == v.pc;
	}

	/**
	 * Check if this variable is a reference to a read/write upvalue
	 *
	 * @param pc   The current PC
	 * @param slot The slot the upvalue is stored in
	 * @return If this is a reference to a read/write upvalue
	 */
	public boolean isUpvalueRefer(int pc, int slot) {
		// special case when both refer and assign in same instruction
		if (pc > 0 && vars[slot][pc] != null && vars[slot][pc].pc == pc && vars[slot][pc - 1] != null) {
			pc -= 1;
		}
		VarInfo v = pc < 0 ? params[slot] : vars[slot][pc];
		return v != null && v.upvalue != null && v.upvalue.readWrite;
	}

	/**
	 * Check if the original value of a slot is used
	 *
	 * @param slot The slot to check
	 * @return If the original variable is referenced
	 */
	public boolean isInitialValueUsed(int slot) {
		return params[slot].isReferenced;
	}

	/**
	 * Check if an upvalue is read/write
	 *
	 * @param u The upvalue to check
	 * @return True if an upvalue is read/write
	 */
	public boolean isReadWriteUpvalue(UpvalueInfo u) {
		return u.readWrite;
	}

	/**
	 * Copy variables from one PC to another
	 *
	 * @param vars   The variables
	 * @param pcFrom The old PC to copy from
	 * @param pcTo   The new PC to copy to
	 */
	private static void propagateVars(VarInfo[][] vars, int pcFrom, int pcTo) {
		for (int j = 0, m = vars.length; j < m; j++) {
			vars[j][pcTo] = vars[j][pcFrom];
		}
	}
}
