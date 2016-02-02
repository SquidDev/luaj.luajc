package org.squiddev.luaj.luajc.analysis.type;

import org.luaj.vm2.Lua;
import org.luaj.vm2.compiler.LuaC;
import org.squiddev.luaj.luajc.analysis.PhiInfo;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.analysis.block.BasicBlock;
import org.squiddev.luaj.luajc.utils.IntArray;

/**
 * Marks where a variable must be converted
 */
public final class ConversionAnnotator {
	public final ProtoInfo info;
	public final boolean[] specialist;

	public ConversionAnnotator(ProtoInfo info) {
		this.info = info;
		this.specialist = new boolean[info.prototype.code.length];

		for (VarInfo param : info.params) {
			markGeneric(param, -1);
		}
	}

	public void fill() {
		int[] code = info.prototype.code;
		for (BasicBlock block : info.blockList) {
			for (int pc = block.pc0; pc <= block.pc1; pc++) {
				specialist[pc] = checkInsn(pc, code[pc]);
			}
		}
	}

	private boolean checkInsn(int pc, int insn) {
		VarInfo[] vars = info.vars[pc];
		switch (Lua.GET_OPCODE(insn)) {
			case Lua.OP_LOADK:     // A Bx    R(A) := Kst(Bx)
			case Lua.OP_LOADBOOL:  // A B  C  R(A) := (Bool)B; if (C) pc++
				markBoth(vars[LuaC.GETARG_A(insn)], pc);
				return true;
			case Lua.OP_NEWTABLE:  // A B  C  R(A) := {} (size = B,C)
			case Lua.OP_GETUPVAL:  // A B     R(A) := UpValue[B]
			case Lua.OP_GETGLOBAL: // A Bx    R(A) := Gbl[Kst(Bx)]
				markGeneric(vars[LuaC.GETARG_A(insn)], pc);
				return false;

			case Lua.OP_UNM:     // A B R(A) := -R(B)
				return unaryOp(vars[LuaC.GETARG_B(insn)], vars[LuaC.GETARG_A(insn)], BasicType.NUMBER, pc);

			case Lua.OP_NOT:     // A B R(A) := not R(B)
				return unaryOp(vars[LuaC.GETARG_B(insn)], vars[LuaC.GETARG_A(insn)], BasicType.BOOLEAN, pc);

			case Lua.OP_LEN:     // A B R(A) := length of R(B)
			case Lua.OP_TESTSET: // A B C if (R(B) <=> C) then R(A) := R(B) else pc++
				// TODO: Optimise TESTSET for specialist types
				ensureGeneric(vars[LuaC.GETARG_A(insn)], pc);
				markGeneric(vars[LuaC.GETARG_B(insn)], pc);
				return false;

			case Lua.OP_ADD: // A B C R(A) := RK(B) + RK(C)
			case Lua.OP_SUB: // A B C R(A) := RK(B) - RK(C)
			case Lua.OP_MUL: // A B C R(A) := RK(B) * RK(C)
			case Lua.OP_DIV: // A B C R(A) := RK(B) / RK(C)
			case Lua.OP_MOD: // A B C R(A) := RK(B) % RK(C)
			case Lua.OP_POW: // A B C R(A) := RK(B) ^ RK(C)
			{
				VarInfo a = vars[LuaC.GETARG_A(insn)];

				int b = LuaC.GETARG_B(insn);
				VarInfo bVar;
				BasicType bType;
				if (Lua.ISK(b)) {
					bVar = null;
					bType = BasicType.fromValue(info.prototype.k[Lua.INDEXK(b)]);
				} else {
					bVar = vars[b];
					bType = bVar.type;
				}

				int c = LuaC.GETARG_C(insn);
				VarInfo cVar;
				BasicType cType;
				if (Lua.ISK(c)) {
					cVar = null;
					cType = BasicType.fromValue(info.prototype.k[Lua.INDEXK(c)]);
				} else {
					cVar = vars[b];
					cType = cVar.type;
				}

				if (bType == BasicType.NUMBER && cType == BasicType.NUMBER) {
					if (bVar != null) ensureSpecialised(bVar, pc);
					if (cVar != null) ensureSpecialised(cVar, pc);
					markMaybe(a, BasicType.NUMBER, pc);
					return true;
				} else {
					if (bVar != null) ensureGeneric(bVar, pc);
					if (cVar != null) ensureGeneric(cVar, pc);
					markGeneric(a, pc);
					return false;
				}
			}

			case Lua.OP_SETTABLE: // A B C R(A)[RK(B)]:= RK(C)
				ensureGeneric(vars[LuaC.GETARG_B(insn)], pc);
				ensureGeneric(vars[LuaC.GETARG_C(insn)], pc);
				ensureGeneric(vars[LuaC.GETARG_A(insn)], pc);
				return false;
			case Lua.OP_CONCAT: // A B C R(A) := R(B) .. ... .. R(C)
			{
				int a = Lua.GETARG_A(insn);
				int b = Lua.GETARG_B(insn);
				int c = Lua.GETARG_C(insn);
				for (; b <= c; b++) {
					ensureGeneric(vars[b], pc);
				}
				markGeneric(vars[a], pc);
				break;
			}
			case Lua.OP_FORPREP: // A sBx R(A)-=R(A+2); pc+=sBx
				// TODO: No clue how to handle things
				return false;
			case Lua.OP_GETTABLE: // A B C R(A) := R(B)[RK(C)]
				ensureGeneric(vars[LuaC.GETARG_B(insn)], pc);
				ensureGeneric(vars[LuaC.GETARG_C(insn)], pc);
				markGeneric(vars[LuaC.GETARG_A(insn)], pc);
				return false;
			case Lua.OP_SELF: // A B C R(A+1) := R(B); R(A) := R(B)[RK(C)]
				ensureGeneric(vars[LuaC.GETARG_B(insn)], pc);
				ensureGeneric(vars[LuaC.GETARG_C(insn)], pc);
				markGeneric(vars[LuaC.GETARG_A(insn)], pc);
				markGeneric(vars[LuaC.GETARG_A(insn) + 1], pc);
				return false;
			case Lua.OP_FORLOOP:
				// TODO: No clue how to handle things
				return false;
			default:
				// throw new IllegalStateException("unhandled opcode: " + insn);
				return false;
		}

		throw new IllegalStateException("Unreachable code!");
	}

	public boolean unaryOp(VarInfo operand, VarInfo product, BasicType required, int pc) {
		// Unary minus can be optimised with a number type
		if (operand.type == required) {
			ensureSpecialised(operand, pc);

			markMaybe(product, required, pc);
			return true;
		} else {
			ensureSpecialised(operand, pc);
			markGeneric(product, pc);
			return false;
		}
	}

	//region Definitions
	private static void markGeneric(VarInfo var, int pc) {
		markType(var, pc, BasicType.VALUE);
	}

	private static void markType(VarInfo var, int pc, BasicType type) {
		var.getTypeInfo().definitions[type.ordinal()].add(pc);
	}

	private static void markSpecialist(VarInfo var, int pc) {
		markType(var, pc, var.type);
	}

	private static void markBoth(VarInfo var, int pc) {
		markType(var, pc, BasicType.VALUE);
		if (var.type != BasicType.VALUE) markType(var, pc, var.type);
	}

	public static void markMaybe(VarInfo var, BasicType required, int pc) {
		if (var.type == required) {
			markSpecialist(var, pc);
		} else {
			markType(var, pc, BasicType.VALUE);
		}
	}

	public void ensure(VarInfo var, int pc, BasicType type) {
		TypeInfo info = var.getTypeInfo();
		int index = type.ordinal();
		info.useFlag |= type.flag();

		IntArray definitions = info.definitions[index];
		if (!definitionDominates(definitions, pc, var.pc)) {
			definitions.add(pc);
		}

		if (var instanceof PhiInfo) {
			PhiInfo phi = (PhiInfo) var;
			for (VarInfo value : phi.values()) {
				TypeInfo valueInfo = value.getTypeInfo();
				IntArray valueDefinitions = valueInfo.definitions[index];
				info.useFlag |= type.flag();
				if (!definitionDominates(valueDefinitions, pc, value.pc)) {
					valueDefinitions.add(phi.pc);
				}
			}
		}
	}

	public void ensureGeneric(VarInfo var, int pc) {
		ensure(var, pc, BasicType.VALUE);
	}

	public void ensureSpecialised(VarInfo var, int pc) {
		ensure(var, pc, var.type);
	}

	public boolean definitionDominates(IntArray defs, int current, int initialDefinition) {
		int size = defs.size();
		int[] items = defs.values();
		for (int i = 0; i < size; i++) {
			int definition = items[i];
			// If the definition occurs initially, then everything is fine
			if (definition == initialDefinition) return true;

			// We are dominated
			if (info.instructionDominates(definition, current)) return true;
		}

		// No dominating definition
		return false;
	}
	//endregion
}
