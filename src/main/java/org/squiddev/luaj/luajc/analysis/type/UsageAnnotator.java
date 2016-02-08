package org.squiddev.luaj.luajc.analysis.type;

import org.luaj.vm2.Lua;
import org.squiddev.luaj.luajc.analysis.PhiInfo;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.analysis.block.BasicBlock;

/**
 * Marks whether instructions can be specialised or not
 */
public final class UsageAnnotator {
	public final ProtoInfo info;
	public final boolean[] specialist;

	public UsageAnnotator(ProtoInfo info) {
		this.info = info;
		this.specialist = new boolean[info.prototype.code.length];
	}

	public void fill() {
		int[] code = info.prototype.code;
		for (BasicBlock block : info.blockList) {
			for (int pc = block.pc0; pc <= block.pc1; pc++) {
				int insn = code[pc];
				specialist[pc] = checkInsn(pc, insn);
				if (Lua.GET_OPCODE(insn) == Lua.OP_CLOSURE) {
					pc += info.prototype.p[Lua.GETARG_Bx(insn)].nups;
				}
			}
		}

		for (PhiInfo phi : info.phis) {
			TypeInfo info = phi.getTypeInfo();
			for (VarInfo var : phi.values()) {
				var.getTypeInfo().absorb(info);
			}
		}
	}

	private boolean checkInsn(int pc, int insn) {
		VarInfo[] vars = info.vars[pc];
		switch (Lua.GET_OPCODE(insn)) {
			case Lua.OP_MOVE:    // A B R(A) := R(B)
			{
				VarInfo a = vars[Lua.GETARG_A(insn)];
				VarInfo b = info.getVariable(pc, Lua.GETARG_B(insn));
				if (b.type == a.type && b.type != BasicType.VALUE) {
					// TODO: Can we optimise this at all?
					b.getTypeInfo().referenceSpecialised(pc);
					return true;
				} else {
					b.getTypeInfo().referenceValue(pc);
					return false;
				}
			}
			case Lua.OP_LOADK:     // A Bx    R(A) := Kst(Bx)
			{
				assert BasicType.fromValue(info.prototype.k[Lua.GETARG_Bx(insn)]) == vars[Lua.GETARG_A(insn)].type : "Incompatible types";
				return true;
			}
			case Lua.OP_LOADBOOL:  // A B  C  R(A) := (Bool)B; if (C) pc++
			case Lua.OP_LOADNIL: // A B R(A) ... R(B) := nil
				return true;
			case Lua.OP_NEWTABLE:  // A B  C  R(A) := {} (size = B,C)
			case Lua.OP_GETUPVAL:  // A B     R(A) := UpValue[B]
			case Lua.OP_GETGLOBAL: // A Bx    R(A) := Gbl[Kst(Bx)]
			case Lua.OP_VARARG: // A B R(A), R(A+1), ..., R(A+B-1) = vararg
			case Lua.OP_CLOSE: // A close all variables in the stack up to (>=) R(A)
			case Lua.OP_JMP: // sBx pc+=sBx
				return false;

			case Lua.OP_UNM:     // A B R(A) := -R(B)
				return unaryOp(info.getVariable(pc, Lua.GETARG_B(insn)), info.getVariable(pc, Lua.GETARG_A(insn)), BasicType.NUMBER, pc);

			case Lua.OP_NOT:     // A B R(A) := not R(B)
				return unaryOp(info.getVariable(pc, Lua.GETARG_B(insn)), info.getVariable(pc, Lua.GETARG_A(insn)), BasicType.BOOLEAN, pc);

			case Lua.OP_LEN:     // A B R(A) := length of R(B)
				vars[Lua.GETARG_A(insn)].getTypeInfo().referenceValue(pc);
				return false;

			case Lua.OP_TESTSET: // A B C if (R(B) <=> C) then R(A) := R(B) else pc++
				// TODO: We need to check if both A and B are booleans
				vars[Lua.GETARG_B(insn)].getTypeInfo().referenceValue(pc);
				return false;

			case Lua.OP_ADD: // A B C R(A) := RK(B) + RK(C)
			case Lua.OP_SUB: // A B C R(A) := RK(B) - RK(C)
			case Lua.OP_MUL: // A B C R(A) := RK(B) * RK(C)
			case Lua.OP_DIV: // A B C R(A) := RK(B) / RK(C)
			case Lua.OP_MOD: // A B C R(A) := RK(B) % RK(C)
			case Lua.OP_POW: // A B C R(A) := RK(B) ^ RK(C)
			{
				int b = Lua.GETARG_B(insn);
				VarInfo bVar;
				BasicType bType;
				if (Lua.ISK(b)) {
					bVar = null;
					bType = BasicType.fromValue(info.prototype.k[Lua.INDEXK(b)]);
				} else {
					bVar = info.getVariable(pc, b);
					bType = bVar.type;
				}

				int c = Lua.GETARG_C(insn);
				VarInfo cVar;
				BasicType cType;
				if (Lua.ISK(c)) {
					cVar = null;
					cType = BasicType.fromValue(info.prototype.k[Lua.INDEXK(c)]);
				} else {
					cVar = info.getVariable(pc, c);
					cType = cVar.type;
				}

				if (bType == BasicType.NUMBER && cType == BasicType.NUMBER) {
					if (bVar != null) bVar.getTypeInfo().referenceSpecialised(pc);
					if (cVar != null) cVar.getTypeInfo().referenceSpecialised(pc);
					return true;
				} else {
					if (bVar != null) bVar.getTypeInfo().referenceValue(pc);
					if (cVar != null) cVar.getTypeInfo().referenceValue(pc);
					return false;
				}
			}

			case Lua.OP_SETTABLE: // A B C R(A)[RK(B)]:= RK(C)
			{
				int b = Lua.GETARG_B(insn), c = Lua.GETARG_C(insn);
				if (!Lua.ISK(b)) info.getVariable(pc, b).getTypeInfo().referenceValue(pc);
				if (!Lua.ISK(c)) info.getVariable(pc, c).getTypeInfo().referenceValue(pc);
				return false;
			}

			case Lua.OP_CONCAT: // A B C R(A) := R(B) .. ... .. R(C)
			{
				int b = Lua.GETARG_B(insn);
				int c = Lua.GETARG_C(insn);
				for (; b <= c; b++) {
					info.getVariable(pc, b).getTypeInfo().referenceValue(pc);
				}
				return false;
			}

			case Lua.OP_FORPREP: // A sBx R(A)-=R(A+2); pc+=sBx
			{
				int a = Lua.GETARG_A(insn);
				VarInfo aVar = info.getVariable(pc, a), aCounter = vars[a + 2];
				if (aVar.type == BasicType.NUMBER && aCounter.type == BasicType.NUMBER) {
					aVar.getTypeInfo().referenceSpecialised(pc);
					aCounter.getTypeInfo().referenceSpecialised(pc);
					return true;
				} else {
					aVar.getTypeInfo().referenceValue(pc);
					aCounter.getTypeInfo().referenceValue(pc);
					return false;
				}
			}

			case Lua.OP_GETTABLE: // A B C R(A) := R(B)[RK(C)]
			case Lua.OP_SELF: // A B C R(A+1) := R(B); R(A) := R(B)[RK(C)]
			{
				info.getVariable(pc, Lua.GETARG_B(insn)).getTypeInfo().referenceValue(pc);
				int c = Lua.GETARG_C(insn);
				if (!Lua.ISK(c)) info.getVariable(pc, c).getTypeInfo().referenceValue(pc);
				return false;
			}

			case Lua.OP_FORLOOP: // A sBx R(A)+=R(A+2); if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }
			{
				int a = Lua.GETARG_A(insn);
				VarInfo counter = info.getVariable(pc, a);
				VarInfo limit = info.getVariable(pc, a + 1);
				VarInfo inc = info.getVariable(pc, a + 2);
				if (counter.type == BasicType.NUMBER && limit.type == BasicType.NUMBER && inc.type == BasicType.NUMBER) {
					counter.getTypeInfo().referenceSpecialised(pc);
					limit.getTypeInfo().referenceSpecialised(pc);
					inc.getTypeInfo().referenceSpecialised(pc);

					return true;
				} else {
					counter.getTypeInfo().referenceValue(pc);
					limit.getTypeInfo().referenceValue(pc);
					inc.getTypeInfo().referenceValue(pc);

					return false;
				}
			}

			case Lua.OP_CALL: // A B C R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))
			case Lua.OP_TAILCALL: // A B C return R(A)(R(A+1), ... ,R(A+B-1))
			{
				int a = Lua.GETARG_A(insn);
				int b = Lua.GETARG_B(insn);

				info.getVariable(pc, a).getTypeInfo().referenceValue(pc);

				int max = b == 0 ? info.prototype.maxstacksize : a + b;
				for (int i = a + 1; i < max; i++) {
					VarInfo info = this.info.getVariable(pc, i);
					if (info == VarInfo.INVALID) break;
					info.getTypeInfo().referenceValue(pc);
				}
				return false;
			}
			case Lua.OP_RETURN: // A B return R(A), ... ,R(A+B-2)
			{
				int a = Lua.GETARG_A(insn);
				int b = Lua.GETARG_B(insn);
				int max = b == 0 ? info.prototype.maxstacksize : a + b - 1;
				for (int i = a; i < max; i++) {
					VarInfo info = vars[i];
					if (info == VarInfo.INVALID) break;
					info.getTypeInfo().referenceValue(pc);
				}
				return false;
			}
			case Lua.OP_TFORLOOP: // A C R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2)); if R(A+3) ~= nil then R(A+2)=R(A+3) else pc++
			{
				int a = Lua.GETARG_A(insn);
				info.getVariable(pc, a++).getTypeInfo().referenceValue(pc);
				info.getVariable(pc, a++).getTypeInfo().referenceValue(pc);
				info.getVariable(pc, a++).getTypeInfo().referenceValue(pc);
				return false;
			}

			case Lua.OP_CLOSURE: // A Bx R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n))
			{
				int b = Lua.GETARG_Bx(insn);
				int nups = info.prototype.p[b].nups;
				for (int k = 1; k <= nups; ++k) {
					int i = info.prototype.code[pc + k];
					if ((i & 4) == 0) {
						vars[Lua.GETARG_B(i)].getTypeInfo().referenceValue(pc);
					}
				}
				return false;
			}

			case Lua.OP_SETLIST: // A B C R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
			{
				int a = Lua.GETARG_A(insn);
				int b = Lua.GETARG_B(insn);
				vars[a].getTypeInfo().referenceValue(pc);

				int max = b == 0 ? info.prototype.maxstacksize : a + b + 1;
				for (int i = a + 1; i < max; i++) {
					VarInfo info = vars[i];
					if (info == VarInfo.INVALID) break;
					info.getTypeInfo().referenceValue(pc);
				}

				return false;
			}

			case Lua.OP_SETGLOBAL: // A Bx Gbl[Kst(Bx)]:= R(A)
			case Lua.OP_SETUPVAL:  // A B  UpValue[B]:= R(A)
			{
				int a = Lua.GETARG_A(insn);
				vars[a].getTypeInfo().referenceValue(pc);
				return false;
			}

			case Lua.OP_TEST:      // A C  if not (R(A) <=> C) then pc++
			{
				TypeInfo info = vars[Lua.GETARG_A(insn)].getTypeInfo();
				if (info.type == BasicType.BOOLEAN) {
					info.referenceSpecialised(pc);
					return true;
				} else {
					info.referenceValue(pc);
					return false;
				}
			}

			case Lua.OP_EQ: // A B C if ((RK(B) == RK(C)) ~= A) then pc++
			case Lua.OP_LT: // A B C if ((RK(B) <  RK(C)) ~= A) then pc++
			case Lua.OP_LE: // A B C if ((RK(B) <= RK(C)) ~= A) then pc++
			{
				int b = Lua.GETARG_B(insn);
				VarInfo bVar;
				BasicType bType;
				if (Lua.ISK(b)) {
					bVar = null;
					bType = BasicType.fromValue(info.prototype.k[Lua.INDEXK(b)]);
				} else {
					bVar = vars[b];
					bType = bVar.type;
				}

				int c = Lua.GETARG_C(insn);
				VarInfo cVar;
				BasicType cType;
				if (Lua.ISK(c)) {
					cVar = null;
					cType = BasicType.fromValue(info.prototype.k[Lua.INDEXK(c)]);
				} else {
					cVar = vars[c];
					cType = cVar.type;
				}

				// We can do == on booleans or numbers, but only < and <= on numbers
				if (bType == cType && insn == Lua.OP_EQ ? bType != BasicType.VALUE : bType == BasicType.NUMBER) {
					if (bVar != null) bVar.getTypeInfo().referenceSpecialised(pc);
					if (cVar != null) cVar.getTypeInfo().referenceSpecialised(pc);
					return true;
				} else {
					if (bVar != null) bVar.getTypeInfo().referenceValue(pc);
					if (cVar != null) cVar.getTypeInfo().referenceValue(pc);
					return false;
				}
			}

			default:
				throw new IllegalStateException("unhandled opcode: " + Lua.GET_OPCODE(insn));
		}
	}

	public boolean unaryOp(VarInfo operand, VarInfo product, BasicType required, int pc) {
		// Unary minus can be optimised with a number type
		if (operand.type == required) {
			product.getTypeInfo().referenceSpecialised(pc);
			return true;
		} else {
			return false;
		}
	}
}
