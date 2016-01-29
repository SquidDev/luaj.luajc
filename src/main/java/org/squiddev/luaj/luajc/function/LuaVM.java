package org.squiddev.luaj.luajc.function;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.DebugLib;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.upvalue.AbstractUpvalue;
import org.squiddev.luaj.luajc.upvalue.ArrayUpvalue;
import org.squiddev.luaj.luajc.upvalue.UpvalueFactory;

import static org.luaj.vm2.LuaValue.NONE;
import static org.luaj.vm2.LuaValue.varargsOf;

/**
 * An implementation of the LuaVM
 */
public final class LuaVM {
	private LuaVM() {
	}

	/**
	 * Run the Lua interpreter on a function
	 *
	 * @param function The function to run
	 * @param stack    The current stack
	 * @param varargs  The arguments passed to it
	 * @return The result
	 */
	public static Varargs execute(FunctionWrapper function, LuaValue[] stack, Varargs varargs) {
		Prototype prototype = function.prototype;

		// Upvalues are only possible when closures create closures
		AbstractUpvalue[] openups = prototype.p.length > 0 ? new AbstractUpvalue[stack.length] : null;

		// Create varargs "arg" table if needed
		if (prototype.is_vararg >= Lua.VARARG_NEEDSARG) stack[prototype.numparams] = new LuaTable(varargs);

		// Push the method call
		LuaThread.CallStack cs = LuaThread.onCall(function);

		return resume(function, stack, varargs, 0, NONE, 0, cs, openups);
	}

	/**
	 * Resume the LuaVM on a particular point
	 *
	 * @param function The function to run
	 * @param stack    The current stack
	 * @param varargs  The arguments passed to it
	 * @param pc       The current PC
	 * @param v        The variable points on the stack
	 * @param top      The top point on the stack
	 * @param cs       The current call stack
	 * @param openups  All openupvalues
	 * @return The result
	 */
	public static Varargs resume(FunctionWrapper function, LuaValue[] stack, Varargs varargs, int pc, Varargs v, int top, LuaThread.CallStack cs, AbstractUpvalue[] openups) {
		// Cache some common features
		ProtoInfo info = function.info;
		Prototype prototype = function.prototype;
		int[] code = prototype.code;
		LuaValue[] constants = prototype.k;
		VarInfo[][] allVars = info.vars;

		// Upvalues are only possible when closures create closures
		AbstractUpvalue[] upvalues = function.upvalues;

		// Debug wants args to this function
		DebugLib.DebugState debugState;
		DebugLib.DebugInfo debugInfo;
		if (DebugLib.DEBUG_ENABLED) {
			DebugLib.debugSetupCall(varargs, stack);
			debugState = DebugLib.getDebugState();
			debugInfo = debugState.getDebugInfo();
		} else {
			debugState = null;
			debugInfo = null;
		}

		{
			VarInfo[] vars = allVars[0];
			for (int i = 0; i < prototype.maxstacksize; i++) {
				vars[i].increment(stack[i]);
			}
		}

		try {
			while (true) {
				if (DebugLib.DEBUG_ENABLED) DebugLib.debugBytecode(debugState, debugInfo, pc, v, top);

				// pull out instruction
				VarInfo[] vars = allVars[pc];
				int i = code[pc];
				pc++;


				int a = Lua.GETARG_A(i);

				// process the op code
				switch (i & 0x3f) {
					case Lua.OP_MOVE: // A B R(A):= R(B)
						vars[a].increment(stack[a] = stack[i >>> 23]);
						continue;

					case Lua.OP_LOADK: // A Bx R(A):= Kst(Bx)
						vars[a].increment(stack[a] = constants[i >>> 14]);
						continue;

					case Lua.OP_LOADBOOL:// A B C R(A):= (Bool)B: if (C) pc++
						vars[a].booleanCount++;
						stack[a] = (i >>> 23 != 0) ? LuaValue.TRUE : LuaValue.FALSE;
						if ((i & (0x1ff << 14)) != 0) {
							pc++; /* skip next instruction (if C) */
						}
						continue;

					case Lua.OP_LOADNIL: // A B R(A):= ...:= R(B):= nil
						for (int b = i >>> 23; a <= b; ) {
							vars[a].valueCount++;
							stack[a++] = LuaValue.NIL;
						}
						continue;

					case Lua.OP_GETUPVAL: // A B R(A):= UpValue[B]
						vars[a].increment(stack[a] = upvalues[i >>> 23].getUpvalue());
						continue;

					case Lua.OP_GETGLOBAL: // A Bx R(A):= Gbl[Kst(Bx)]
						vars[a].increment(stack[a] = function.getfenv().get(constants[i >>> 14]));
						continue;

					case Lua.OP_GETTABLE: // A B C R(A):= R(B)[RK(C)]
					{
						int c = Lua.GETARG_C(i);
						vars[a].increment(stack[a] = stack[i >>> 23].get(c > 0xff ? constants[c & 0x0ff] : stack[c]));
						continue;
					}

					case Lua.OP_SETGLOBAL: // A Bx Gbl[Kst(Bx)]:= R(A)
						function.getfenv().set(constants[i >>> 14], stack[a]);
						continue;

					case Lua.OP_SETUPVAL: // A B UpValue[B]:= R(A)
						upvalues[i >>> 23].setUpvalue(stack[a]);
						continue;

					case Lua.OP_SETTABLE: // A B C R(A)[RK(B)]:= RK(C)
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						stack[a].set((b > 0xff ? constants[b & 0x0ff] : stack[b]), c > 0xff ? constants[c & 0x0ff] : stack[c]);
						continue;
					}

					case Lua.OP_NEWTABLE: // A B C R(A):= {} (size = B,C)
						vars[a].valueCount++;
						stack[a] = new LuaTable(i >>> 23, (i >> 14) & 0x1ff);
						continue;

					case Lua.OP_SELF: // A B C R(A+1):= R(B): R(A):= R(B)[RK(C)]
					{
						int c = Lua.GETARG_C(i);
						LuaValue table = stack[a + 1] = stack[i >>> 23];
						vars[a + 1].increment(table);
						vars[a].increment(stack[a] = table.get(c > 0xff ? constants[c & 0x0ff] : stack[c]));
						continue;
					}

					case Lua.OP_ADD: // A B C R(A):= RK(B) + RK(C)
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						vars[a].increment(stack[a] = (b > 0xff ? constants[b & 0x0ff] : stack[b]).add(c > 0xff ? constants[c & 0x0ff] : stack[c]));
						continue;
					}
					case Lua.OP_SUB: // A B C R(A):= RK(B) - RK(C)
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						vars[a].increment(stack[a] = (b > 0xff ? constants[b & 0x0ff] : stack[b]).sub(c > 0xff ? constants[c & 0x0ff] : stack[c]));
						continue;
					}

					case Lua.OP_MUL: // A B C R(A):= RK(B) * RK(C)
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						vars[a].increment(stack[a] = (b > 0xff ? constants[b & 0x0ff] : stack[b]).mul(c > 0xff ? constants[c & 0x0ff] : stack[c]));
						continue;
					}

					case Lua.OP_DIV: // A B C R(A):= RK(B) / RK(C)
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						vars[a].increment(stack[a] = (b > 0xff ? constants[b & 0x0ff] : stack[b]).div(c > 0xff ? constants[c & 0x0ff] : stack[c]));
						continue;
					}

					case Lua.OP_MOD: // A B C R(A):= RK(B) % RK(C)
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						vars[a].increment(stack[a] = (b > 0xff ? constants[b & 0x0ff] : stack[b]).mod(c > 0xff ? constants[c & 0x0ff] : stack[c]));
						continue;
					}

					case Lua.OP_POW: // A B C R(A):= RK(B) ^ RK(C)
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						vars[a].increment(stack[a] = (b > 0xff ? constants[b & 0x0ff] : stack[b]).pow(c > 0xff ? constants[c & 0x0ff] : stack[c]));
						continue;
					}

					case Lua.OP_UNM: // A B R(A):= -R(B)
						vars[a].increment(stack[a] = stack[i >>> 23].neg());
						continue;

					case Lua.OP_NOT: // A B R(A):= not R(B)
						vars[a].increment(stack[a] = stack[i >>> 23].not());
						continue;

					case Lua.OP_LEN: // A B R(A):= length of R(B)
						vars[a].increment(stack[a] = stack[i >>> 23].len());
						continue;

					case Lua.OP_CONCAT: // A B C R(A):= R(B).. ... ..R(C)
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						if (c > b + 1) {
							Buffer sb = stack[c].buffer();
							while (--c >= b) {
								sb = stack[c].concat(sb);
							}
							vars[a].increment(stack[a] = sb.value());
						} else {
							vars[a].increment(stack[a] = stack[c - 1].concat(stack[c]));
						}
					}
					continue;

					case Lua.OP_JMP: // sBx pc+=sBx
						pc += (i >>> 14) - 0x1ffff;
						continue;

					case Lua.OP_EQ: // A B C if ((RK(B) == RK(C)) ~= A) then pc++
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						if ((b > 0xff ? constants[b & 0x0ff] : stack[b]).eq_b(c > 0xff ? constants[c & 0x0ff] : stack[c]) != (a != 0)) {
							++pc;
						}
						continue;
					}

					case Lua.OP_LT: // A B C if ((RK(B) <  RK(C)) ~= A) then pc++
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						if ((b > 0xff ? constants[b & 0x0ff] : stack[b]).lt_b(c > 0xff ? constants[c & 0x0ff] : stack[c]) != (a != 0)) {
							++pc;
						}
						continue;
					}

					case Lua.OP_LE: // A B C if ((RK(B) <= RK(C)) ~= A) then pc++
					{
						int b = i >>> 23;
						int c = Lua.GETARG_C(i);
						if ((b > 0xff ? constants[b & 0x0ff] : stack[b]).lteq_b(c > 0xff ? constants[c & 0x0ff] : stack[c]) != (a != 0)) {
							++pc;
						}
						continue;
					}

					case Lua.OP_TEST: // A C if not (R(A) <=> C) then pc++
						if (stack[a].toboolean() != ((i & (0x1ff << 14)) != 0)) {
							++pc;
						}
						continue;

					case Lua.OP_TESTSET: // A B C if (R(B) <=> C) then R(A):= R(B) else pc++

					{
						/* note: doc appears to be reversed */
						LuaValue test = stack[i >>> 23];
						if (test.toboolean() != ((i & (0x1ff << 14)) != 0)) {
							++pc;
						} else {
							vars[a].increment(stack[a] = test);
						}
						continue;
					}

					case Lua.OP_CALL: // A B C R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1))
						switch (i & (Lua.MASK_B | Lua.MASK_C)) {
							case (1 << Lua.POS_B) | (0 << Lua.POS_C):
								v = stack[a].invoke(NONE);
								top = a + v.narg();
								continue;
							case (2 << Lua.POS_B) | (0 << Lua.POS_C):
								v = stack[a].invoke(stack[a + 1]);
								top = a + v.narg();
								continue;
							case (1 << Lua.POS_B) | (1 << Lua.POS_C):
								stack[a].call();
								continue;
							case (2 << Lua.POS_B) | (1 << Lua.POS_C):
								stack[a].call(stack[a + 1]);
								continue;
							case (3 << Lua.POS_B) | (1 << Lua.POS_C):
								stack[a].call(stack[a + 1], stack[a + 2]);
								continue;
							case (4 << Lua.POS_B) | (1 << Lua.POS_C):
								stack[a].call(stack[a + 1], stack[a + 2], stack[a + 3]);
								continue;
							case (1 << Lua.POS_B) | (2 << Lua.POS_C):
								vars[a].increment(stack[a] = stack[a].call());
								continue;
							case (2 << Lua.POS_B) | (2 << Lua.POS_C):
								vars[a].increment(stack[a] = stack[a].call(stack[a + 1]));
								continue;
							case (3 << Lua.POS_B) | (2 << Lua.POS_C):
								vars[a].increment(stack[a] = stack[a].call(stack[a + 1], stack[a + 2]));
								continue;
							case (4 << Lua.POS_B) | (2 << Lua.POS_C):
								vars[a].increment(stack[a] = stack[a].call(stack[a + 1], stack[a + 2], stack[a + 3]));
								continue;
							default: {
								int b = i >>> 23;
								int c = Lua.GETARG_C(i);
								v = b > 0 ?
									varargsOf(stack, a + 1, b - 1) : // exact arg count
									varargsOf(stack, a + 1, top - v.narg() - (a + 1), v); // from prev top
								v = stack[a].invoke(v);
								if (c > 0) {
									while (--c > 0) {
										int indx = a + c - 1;
										vars[indx].increment(stack[indx] = v.arg(c));
									}
									v = NONE; // TODO: necessary?
								} else {
									top = a + v.narg();
								}
								continue;
							}
						}

					case Lua.OP_TAILCALL: // A B C return R(A)(R(A+1), ... ,R(A+B-1))
						switch (i & Lua.MASK_B) {
							case (1 << Lua.POS_B):
								return new TailcallVarargs(stack[a], NONE);
							case (2 << Lua.POS_B):
								return new TailcallVarargs(stack[a], stack[a + 1]);
							case (3 << Lua.POS_B):
								return new TailcallVarargs(stack[a], varargsOf(stack[a + 1], stack[a + 2]));
							case (4 << Lua.POS_B):
								return new TailcallVarargs(stack[a], varargsOf(stack[a + 1], stack[a + 2], stack[a + 3]));
							default: {
								int b = i >>> 23;
								v = b > 0 ?
									varargsOf(stack, a + 1, b - 1) : // exact arg count
									varargsOf(stack, a + 1, top - v.narg() - (a + 1), v); // from prev top
								return new TailcallVarargs(stack[a], v);
							}
						}

					case Lua.OP_RETURN: // A B return R(A), ... ,R(A+B-2) (see note)
					{
						int b = i >>> 23;
						switch (b) {
							case 0:
								return varargsOf(stack, a, top - v.narg() - a, v);
							case 1:
								return NONE;
							case 2:
								return stack[a];
							default:
								return varargsOf(stack, a, b - 1);
						}
					}

					case Lua.OP_FORLOOP: // A sBx R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
					{
						LuaValue limit = stack[a + 1];
						LuaValue step = stack[a + 2];
						LuaValue idx = step.add(stack[a]);
						if (step.gt_b(0) ? idx.lteq_b(limit) : idx.gteq_b(limit)) {
							vars[a].increment(stack[a] = idx);
							vars[a + 3].increment(stack[a + 3] = idx);
							pc += (i >>> 14) - 0x1ffff;
						}
					}
					continue;

					case Lua.OP_FORPREP: // A sBx R(A)-=R(A+2): pc+=sBx
					{
						LuaValue init = stack[a].checknumber("'for' initial value must be a number");
						LuaValue limit = stack[a + 1].checknumber("'for' limit must be a number");
						LuaValue step = stack[a + 2].checknumber("'for' step must be a number");
						vars[a].increment(stack[a] = init.sub(step));
						vars[a + 1].increment(stack[a + 1] = limit);
						vars[a + 2].increment(stack[a + 2] = step);
						pc += (i >>> 14) - 0x1ffff;
					}
					continue;

					case Lua.OP_TFORLOOP: {
						/*
							A C R(A+3), ... ,R(A+2+C):= R(A)(R(A+1),
							R(A+2)): if R(A+3) ~= nil then R(A+2)=R(A+3)
							else pc++
						*/
						// TODO: stack call on for loop body, such as:   stack[a].call(ci);
						v = stack[a].invoke(varargsOf(stack[a + 1], stack[a + 2]));
						LuaValue result = v.arg1();
						if (result.isnil()) {
							++pc;
						} else {
							stack[a + 2] = stack[a + 3] = result;
							vars[a + 2].increment(result);
							vars[a + 3].increment(result);
							for (int c = Lua.GETARG_C(i); c > 1; --c) {
								int idx = a + 2 + c;
								vars[idx].increment(stack[idx] = v.arg(c));
							}
							v = NONE; // todo: necessary?
						}
						continue;
					}

					case Lua.OP_SETLIST: // A B CR(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
					{
						int c = Lua.GETARG_C(i);
						if (c == 0) {
							c = code[pc++];
						}
						int offset = (c - 1) * Lua.LFIELDS_PER_FLUSH;
						LuaValue table = stack[a];
						int b = i >>> 23;
						if (b == 0) {
							b = top - a - 1;
							int m = b - v.narg();
							int j = 1;
							for (; j <= m; j++) {
								table.set(offset + j, stack[a + j]);
							}
							for (; j <= b; j++) {
								table.set(offset + j, v.arg(j - m));
							}
						} else {
							table.presize(offset + b);
							for (int j = 1; j <= b; j++) {
								table.set(offset + j, stack[a + j]);
							}
						}
					}
					continue;

					case Lua.OP_CLOSE: // A close all variables in the stack up to (>=) R(A)
						for (int b = openups.length; --b >= a; ) {
							if (openups[b] != null) {
								openups[b].close();
								openups[b] = null;
							}
						}
						continue;

					case Lua.OP_CLOSURE: // A Bx R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
					{
						ProtoInfo newp = info.subprotos[i >>> 14];
						int nups = newp.prototype.nups;
						FunctionWrapper newcl = new FunctionWrapper(newp, function.getfenv());
						vars[a].valueCount++;
						for (int j = 0; j < nups; ++j) {
							i = code[pc++];

							int b = i >>> 23;
							if ((i & 4) != 0) {
								newcl.upvalues[j] = upvalues[b];
							} else {
								AbstractUpvalue upvalue = openups[b];
								if (upvalue == null) {
									// We need the proxy upvalue for closing it
									upvalue = openups[b] = UpvalueFactory.proxy(new ArrayUpvalue(stack, b));
								}
								newcl.upvalues[j] = upvalue;
							}
						}
						stack[a] = newcl;
					}
					continue;

					case Lua.OP_VARARG: // A B R(A), R(A+1), ..., R(A+B-1) = vararg
					{
						int b = i >>> 23;
						if (b == 0) {
							top = a + varargs.narg();
							v = varargs;
						} else {
							for (int j = 1; j < b; ++j) {
								int idx = a + j - 1;
								vars[idx].increment(stack[idx] = varargs.arg(j));
							}
						}
					}
				}
			}
		} catch (LuaError le) {
			throw le;
		} catch (Exception e) {
			throw new LuaError(e);
		} finally {
			cs.onReturn();
			if (openups != null) {
				for (int u = openups.length; --u >= 0; ) {
					if (openups[u] != null) {
						openups[u].close();
					}
				}
			}
		}
	}
}
