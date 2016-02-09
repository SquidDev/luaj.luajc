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
package org.squiddev.luaj.luajc.compilation;

import org.luaj.vm2.Lua;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.analysis.block.BasicBlock;
import org.squiddev.luaj.luajc.analysis.block.BranchVisitor;
import org.squiddev.luaj.luajc.analysis.type.BasicType;
import org.squiddev.luaj.luajc.analysis.type.TypeAnnotator;
import org.squiddev.luaj.luajc.analysis.type.TypeInfo;
import org.squiddev.luaj.luajc.analysis.type.UsageAnnotator;

public final class JavaGen {
	public final byte[] bytecode;
	public final ProtoInfo prototype;

	public JavaGen(ProtoInfo pi, JavaLoader loader, String filename) {
		prototype = pi;

		// Analysis
		new TypeAnnotator(pi).fill(loader.options.compileThreshold);
		UsageAnnotator usage = new UsageAnnotator(pi);
		usage.fill();

		// build this class
		JavaBuilder builder = new JavaBuilder(pi, loader.options.prefix + loader.name, filename);
		scanInstructions(pi, builder, usage);
		bytecode = builder.completeClass();
	}

	private void scanInstructions(ProtoInfo pi, JavaBuilder builder, UsageAnnotator usage) {
		Prototype p = pi.prototype;
		int vresultbase = -1;

		builder.initializeSlots(true);

		for (BasicBlock b0 : pi.blockList) {
			boolean specialistBlock = usage.specialistBlocks.contains(b0);
			for (int pc = b0.pc0; pc <= b0.pc1; pc++) {
				int insn = p.code[pc];
				boolean specialised = usage.specialist[pc];

				if (!specialistBlock) builder.visitGenericLabel(pc);
				builder.onStartOfLuaInstruction(pc, true);

				// For each block we should create upvalues for them. We have to do this after the first instruction
				if (pc == b0.pc0) setUpvalues(pi, b0, builder);

				vresultbase = scanInstruction(pi, builder, insn, pc, specialised, true, vresultbase);

				if (Lua.GET_OPCODE(insn) == Lua.OP_CLOSURE) {
					pc += p.p[Lua.GETARG_Bx(insn)].nups;
				}
			}

			if (!BranchVisitor.isTerminator(Lua.GET_OPCODE(p.code[b0.pc1]))) {
				assert b0.next != null && b0.next.length == 1 : "Expected 1 successor for " + b0;
				builder.setupPhis(b0.pc1, b0.next[0].pc0);
			}
		}

		vresultbase = -1;

		if (usage.paramsSpecialised) builder.initializeSlots(false);

		for (BasicBlock b0 : pi.blockList) {
			if (usage.specialistBlocks.contains(b0)) {
				for (int pc = b0.pc0; pc <= b0.pc1; pc++) {
					int insn = p.code[pc];

					builder.onStartOfLuaInstruction(pc, false);

					// For each block we should create upvalues for them. We have to do this after the first instruction
					if (pc == b0.pc0) setUpvalues(pi, b0, builder);

					builder.visitResumeLabel(pc);
					vresultbase = scanInstruction(pi, builder, insn, pc, false, false, vresultbase);

					if (Lua.GET_OPCODE(insn) == Lua.OP_CLOSURE) {
						pc += p.p[Lua.GETARG_Bx(insn)].nups;
					}
				}

				if (!BranchVisitor.isTerminator(Lua.GET_OPCODE(p.code[b0.pc1]))) {
					assert b0.next != null && b0.next.length == 1 : "Expected 1 successor for " + b0;
					builder.addBranch(JavaBuilder.BRANCH_GOTO, b0.next[0].pc0, b0.pc1, false);
				}
			}
		}
	}

	private void setUpvalues(ProtoInfo pi, BasicBlock block, JavaBuilder builder) {
		// convert upvalues that are phi-variables

		Prototype p = pi.prototype;
		for (int slot = 0; slot < p.maxstacksize; slot++) {
			int up_pc = block.pc0;
			boolean c = pi.getVariable(up_pc, slot).isUpvalueCreate(up_pc);
			if (c && pi.vars[up_pc][slot].isPhiVar()) {
				builder.convertToUpvalue(up_pc, slot);
			}
		}
	}

	private int scanInstruction(ProtoInfo pi, JavaBuilder builder, int ins, int pc, boolean insnSpecialise, boolean blockSpecialise, int vresultbase) {
		Prototype p = pi.prototype;

		final int o = Lua.GET_OPCODE(ins);
		int a = Lua.GETARG_A(ins);
		int b = Lua.GETARG_B(ins);
		int bx = Lua.GETARG_Bx(ins);
		int sbx = Lua.GETARG_sBx(ins);
		int c = Lua.GETARG_C(ins);

		VariableLoader loader = blockSpecialise ? builder.specialisedLoader : builder.genericLoader;

		switch (o) {
			case Lua.OP_GETUPVAL: // A B R(A):= UpValue[B]
				builder.loadUpvalue(b);
				loader.storeLocal(pc, a, BasicType.VALUE);
				break;

			case Lua.OP_SETUPVAL: // A B UpValue[B]:= R(A)
				builder.storeUpvalue(pc, b, a);
				break;

			case Lua.OP_NEWTABLE: // A B C R(A):= {} (size = B,C)
				builder.newTable(b, c);
				loader.storeLocal(pc, a, BasicType.VALUE);
				break;

			case Lua.OP_MOVE:// A B R(A):= R(B)
			{
				VarInfo var = prototype.getVariable(pc, b);
				loader.loadLocal(pc, b, insnSpecialise);
				loader.storeLocal(pc, a, insnSpecialise ? var.type : BasicType.VALUE);
				break;
			}

			case Lua.OP_UNM: // A B R(A):= -R(B)
			case Lua.OP_NOT: // A B R(A):= not R(B)
			case Lua.OP_LEN: // A B R(A):= length of R(B)
			{
				VarInfo var = prototype.getVariable(pc, b);
				loader.loadLocal(var, insnSpecialise);
				builder.unaryOp(o, insnSpecialise);
				loader.storeLocal(pc, a, insnSpecialise ? var.type : BasicType.VALUE);
				break;
			}

			case Lua.OP_LOADK:// A Bx R(A):= Kst(Bx)
			{
				TypeInfo info = pi.vars[pc][a].getTypeInfo();
				if (blockSpecialise && info.specialisedReferenced) {
					builder.loadConstant(p.k[bx], true);
					loader.storeLocalNoChecks(pc, a, true);
				}

				if (!blockSpecialise || info.valueReferenced) {
					builder.loadConstant(p.k[bx], false);
					loader.storeLocalNoChecks(pc, a, false);
				}

				assert (info.valueReferenced || info.specialisedReferenced) == pi.vars[pc][a].isReferenced : "Referenced are not the same " + pi.vars[pc][a];

				break;
			}

			case Lua.OP_GETGLOBAL: // A Bx R(A):= Gbl[Kst(Bx)]
				builder.loadEnv();
				builder.loadConstant(p.k[bx], false);
				builder.getTable();
				loader.storeLocal(pc, a, BasicType.VALUE);
				break;

			case Lua.OP_SETGLOBAL: // A Bx Gbl[Kst(Bx)]:= R(A)
				builder.loadEnv();
				builder.loadConstant(p.k[bx], false);
				loader.loadLocal(pc, a, false);
				builder.setTable();
				break;

			case Lua.OP_LOADNIL: // A B R(A):= ...:= R(B):= nil
				builder.loadNil();
				for (; a <= b; a++) {
					if (a < b) {
						builder.dup();
					}
					loader.storeLocal(pc, a, BasicType.VALUE);
				}
				break;

			case Lua.OP_GETTABLE: // A B C R(A):= R(B)[RK(C)]
				loader.loadLocal(pc, b, false);
				builder.loadLocalOrConstant(pc, c, false);
				builder.getTable();
				loader.storeLocal(pc, a, BasicType.VALUE);
				break;

			case Lua.OP_SETTABLE: // A B C R(A)[RK(B)]:= RK(C)
				loader.loadLocal(pc, a, false);
				builder.loadLocalOrConstant(pc, b, false);
				builder.loadLocalOrConstant(pc, c, false);
				builder.setTable();
				break;

			case Lua.OP_ADD: // A B C R(A):= RK(B) + RK(C)
			case Lua.OP_SUB: // A B C R(A):= RK(B) - RK(C)
			case Lua.OP_MUL: // A B C R(A):= RK(B) * RK(C)
			case Lua.OP_DIV: // A B C R(A):= RK(B) / RK(C)
			case Lua.OP_MOD: // A B C R(A):= RK(B) % RK(C)
			case Lua.OP_POW: // A B C R(A):= RK(B) ^ RK(C)
				builder.loadLocalOrConstant(pc, b, insnSpecialise);
				builder.loadLocalOrConstant(pc, c, insnSpecialise);
				builder.binaryOp(o, insnSpecialise);
				loader.storeLocal(pc, a, insnSpecialise ? BasicType.NUMBER : BasicType.VALUE);
				break;

			case Lua.OP_SELF: // A B C R(A+1):= R(B): R(A):= R(B)[RK(C)]
				loader.loadLocal(pc, b, false);
				builder.dup();
				loader.storeLocal(pc, a + 1, BasicType.VALUE);
				builder.loadLocalOrConstant(pc, c, false);
				builder.getTable();
				loader.storeLocal(pc, a, BasicType.VALUE);
				break;

			case Lua.OP_CONCAT: // A B C R(A):= R(B).. ... ..R(C)
				for (int k = b; k <= c; k++) {
					loader.loadLocal(pc, k, false);
				}
				if (c > b + 1) {
					builder.visitTobuffer();
					for (int k = c; --k >= b; ) {
						builder.visitConcatBuffer();
					}
					builder.visitTovalue();
				} else {
					builder.visitConcatValue();
				}
				loader.storeLocal(pc, a, BasicType.VALUE);
				break;

			case Lua.OP_LOADBOOL:// A B C R(A):= (Bool)B: if (C) pc++
			{
				LuaValue value = b != 0 ? LuaValue.TRUE : LuaValue.FALSE;
				TypeInfo info = pi.vars[pc][a].getTypeInfo();

				if (blockSpecialise && info.specialisedReferenced) {
					builder.loadConstant(value, true);
					loader.storeLocalNoChecks(pc, a, true);
				}

				if (!blockSpecialise || info.valueReferenced) {
					builder.loadConstant(value, false);
					loader.storeLocalNoChecks(pc, a, false);
				}

				if (c != 0) {
					builder.addBranch(JavaBuilder.BRANCH_GOTO, pc + 2, pc, blockSpecialise);
				}
				break;
			}

			case Lua.OP_JMP: // sBx pc+=sBx
				builder.addBranch(JavaBuilder.BRANCH_GOTO, pc + 1 + sbx, pc, blockSpecialise);
				break;

			case Lua.OP_EQ: // A B C if ((RK(B) == RK(C)) ~= A) then pc++
			case Lua.OP_LT: // A B C if ((RK(B) <  RK(C)) ~= A) then pc++
			case Lua.OP_LE: // A B C if ((RK(B) <= RK(C)) ~= A) then pc++
				builder.loadLocalOrConstant(pc, b, insnSpecialise);
				builder.loadLocalOrConstant(pc, c, insnSpecialise);
				builder.compareOp(o, insnSpecialise, a != 0, pc + 2, pc, blockSpecialise);
				break;

			case Lua.OP_TEST: // A C if not (R(A) <=> C) then pc++
				loader.loadLocal(pc, a, insnSpecialise);
				if (!insnSpecialise) builder.visitToBoolean();
				builder.addBranch((c != 0 ? JavaBuilder.BRANCH_IFEQ : JavaBuilder.BRANCH_IFNE), pc + 2, pc, blockSpecialise);
				break;

			case Lua.OP_TESTSET: // A B C if (R(B) <=> C) then R(A):= R(B) else pc++
				loader.loadLocal(pc, b, insnSpecialise);
				if (!insnSpecialise) builder.visitToBoolean();
				builder.addBranch((c != 0 ? JavaBuilder.BRANCH_IFEQ : JavaBuilder.BRANCH_IFNE), pc + 2, pc, blockSpecialise);

				loader.loadLocal(pc, b, false);
				loader.storeLocal(pc, a, BasicType.VALUE);
				break;

			case Lua.OP_CALL: // A B C R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1))
				// load function
				loader.loadLocal(pc, a, false);

				// load args
				int narg = b - 1;
				switch (narg) {
					case 0:
					case 1:
					case 2:
					case 3:
						for (int i = 1; i < b; i++) {
							loader.loadLocal(pc, a + i, false);
						}
						break;
					default: // fixed arg count > 3
						builder.newVarargs(pc, a + 1, b - 1);
						narg = -1;
						break;
					case -1: // prev vararg result
						builder.loadVarargResults(pc, a + 1, vresultbase);
						narg = -1;
						break;
				}

				// call or invoke
				boolean useinvoke = narg < 0 || c < 1 || c > 2;
				if (useinvoke) {
					builder.invoke(narg);
				} else {
					builder.call(narg);
				}

				// handle results
				switch (c) {
					case 1:
						builder.pop();
						break;
					case 2:
						if (useinvoke) {
							builder.arg(1);
						}
						loader.storeLocal(pc, a, BasicType.VALUE);
						break;
					default: // fixed result count - unpack args
						for (int i = 1; i < c; i++) {
							if (i + 1 < c) {
								builder.dup();
							}
							builder.arg(i);
							VarInfo var = pi.vars[pc][a + i - 1];
							if (var.type == BasicType.VALUE || !var.getTypeInfo().specialisedReferenced) {
								loader.storeLocal(var, BasicType.VALUE);
							} else {
								loader.storeLocalNoChecks(var, false);
							}
						}
						for (int i = 1; i < c; i++) {
							VarInfo var = pi.vars[pc][a + i - 1];
							if (var.type != BasicType.VALUE && var.getTypeInfo().specialisedReferenced) {
								loader.refreshLocal(var, pc + 1);
							}
						}
						break;
					case 0: // vararg result
						vresultbase = a;
						builder.storeVarResult();
						break;
				}
				break;

			case Lua.OP_TAILCALL: // A B C return R(A)(R(A+1), ... ,R(A+B-1))
				// load function
				loader.loadLocal(pc, a, false);

				// load args
				switch (b) {
					case 1:
						builder.loadNone();
						break;
					case 2:
						loader.loadLocal(pc, a + 1, false);
						break;
					default: // fixed arg count > 1
						builder.newVarargs(pc, a + 1, b - 1);
						break;
					case 0: // prev vararg result
						builder.loadVarargResults(pc, a + 1, vresultbase);
						break;
				}
				builder.newTailcallVarargs();
				builder.visitReturn();
				break;

			case Lua.OP_RETURN: // A B return R(A), ... ,R(A+B-2)	(see note)
				if (c == 1) {
					builder.loadNone();
				} else {
					switch (b) {
						case 0:
							builder.loadVarargResults(pc, a, vresultbase);
							break;
						case 1:
							builder.loadNone();
							break;
						case 2:
							loader.loadLocal(pc, a, false);
							break;
						default:
							builder.newVarargs(pc, a, b - 1);
							break;
					}
				}
				builder.visitReturn();
				break;

			case Lua.OP_FORPREP: // A sBx R(A)-=R(A+2): pc+=sBx
				loader.loadLocal(pc, a, insnSpecialise);
				loader.loadLocal(pc, a + 2, insnSpecialise);
				builder.binaryOp(Lua.OP_SUB, insnSpecialise);
				loader.storeLocal(pc, a, insnSpecialise ? BasicType.NUMBER : BasicType.VALUE);
				builder.addBranch(JavaBuilder.BRANCH_GOTO, pc + 1 + sbx, pc, blockSpecialise);
				break;

			case Lua.OP_FORLOOP: // A sBx R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
			{
				loader.loadLocal(pc, a, insnSpecialise);
				loader.loadLocal(pc, a + 2, insnSpecialise);
				builder.binaryOp(Lua.OP_ADD, insnSpecialise);
				BasicType type = insnSpecialise ? BasicType.NUMBER : BasicType.VALUE;
				builder.dup(type);
				builder.dup(type);
				loader.storeLocal(pc, a, type);
				loader.storeLocal(pc, a + 3, type);
				loader.loadLocal(pc, a + 1, insnSpecialise); // limit
				loader.loadLocal(pc, a + 2, insnSpecialise); // step
				builder.testForLoop(insnSpecialise);
				builder.addBranch(JavaBuilder.BRANCH_IFNE, pc + 1 + sbx, pc, blockSpecialise);
				break;
			}

			case Lua.OP_TFORLOOP:
						/*
						 	A C R(A+3), ... ,R(A+2+C):= R(A)(R(A+1),
						 	R(A+2)): if R(A+3) ~= nil then R(A+2)=R(A+3)
						 	else pc++
						*/
				// v = stack[a].invoke(varargsOf(stack[a+1],stack[a+2]));
				// if ( (o=v.arg1()).isnil() )
				//	++pc;
				loader.loadLocal(pc, a, false);
				loader.loadLocal(pc, a + 1, false);
				loader.loadLocal(pc, a + 2, false);
				builder.invoke(2); // varresult on stack
				builder.dup();
				builder.storeVarResult();
				builder.arg(1);
				builder.visitIsNil();
				builder.addBranch(JavaBuilder.BRANCH_IFNE, pc + 2, pc, blockSpecialise);

				// a[2] = a[3] = v[1], leave varargs on stack
				builder.createUpvalues(pc, a + 3, c);
				builder.loadVarResult();
				if (c >= 2) {
					builder.dup();
				}
				builder.arg(1);
				builder.dup();
				loader.storeLocal(pc, a + 2, BasicType.VALUE);
				loader.storeLocal(pc, a + 3, BasicType.VALUE);

				// v[2]..v[c], use varargs from stack
				for (int j = 2; j <= c; j++) {
					if (j < c) {
						builder.dup();
					}
					builder.arg(j);
					loader.storeLocal(pc, a + 2 + j, BasicType.VALUE);
				}
				break;

			case Lua.OP_SETLIST: // A B C R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
			{
				int index0 = (c - 1) * Lua.LFIELDS_PER_FLUSH + 1;
				loader.loadLocal(pc, a, false);
				if (b == 0) {
					int nstack = vresultbase - (a + 1);
					if (nstack > 0) {
						builder.visitSetlistStack(pc, a + 1, index0, nstack);
						index0 += nstack;
					}
					builder.visitSetlistVarargs(index0);
				} else {
					builder.visitSetlistStack(pc, a + 1, index0, b);
					builder.pop();
				}
				break;
			}

			case Lua.OP_CLOSE: // A  close all variables in the stack up to (>=) R(A)
				break;

			case Lua.OP_CLOSURE: // A Bx R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
			{
				ProtoInfo newp = pi.subprotos[bx];
				int nup = newp.upvalues == null ? 0 : newp.upvalues.length;
				builder.closureCreate(newp);
				if (nup > 0) builder.dup();
				loader.storeLocal(pc, a, BasicType.VALUE);

				if (nup > 0) {
					builder.upvaluesGet();
					for (int up = 0; up < nup; ++up) {
						if (up + 1 < nup) builder.dup();
						ins = p.code[pc + up + 1];
						b = Lua.GETARG_B(ins);
						if ((ins & 4) != 0) {
							builder.initUpvalueFromUpvalue(up, b);
						} else {
							builder.initUpvalueFromLocal(up, pc, b);
						}
					}
				}
				break;
			}
			case Lua.OP_VARARG: // A B R(A), R(A+1), ..., R(A+B-1) = vararg
				if (b == 0) {
					builder.loadVarargs();
					builder.storeVarResult();
					vresultbase = a;
				} else {
					for (int i = 1; i < b; i++) {
						builder.loadVarargs(i);
						VarInfo var = pi.vars[pc][a + i - 1];
						if (var.type == BasicType.VALUE || !var.getTypeInfo().specialisedReferenced) {
							loader.storeLocal(var, BasicType.VALUE);
						} else {
							loader.storeLocalNoChecks(var, false);
						}
					}
					for (int i = 1; i < b; i++) {
						VarInfo var = pi.vars[pc][a + i - 1];
						if (var.type != BasicType.VALUE && var.getTypeInfo().specialisedReferenced) {
							loader.refreshLocal(var, pc + 1);
						}
					}
				}
				break;
		}

		return vresultbase;
	}
}
