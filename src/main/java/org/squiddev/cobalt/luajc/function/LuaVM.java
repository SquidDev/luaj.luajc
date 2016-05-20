package org.squiddev.cobalt.luajc.function;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LuaInterpreter;
import org.squiddev.cobalt.luajc.analysis.ProtoInfo;
import org.squiddev.cobalt.luajc.upvalue.AbstractUpvalue;
import org.squiddev.cobalt.luajc.upvalue.ArrayUpvalue;
import org.squiddev.cobalt.luajc.utils.TypeFactory;

import static org.squiddev.cobalt.Constants.FALSE;
import static org.squiddev.cobalt.Constants.TRUE;
import static org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * An implementation of the LuaVM.
 *
 * @see LuaInterpreter
 */
public final class LuaVM {
	private LuaVM() {
	}

	public static Varargs execute(LuaState state, FunctionWrapper function, LuaValue[] stack, Varargs varargs, int pc) {
		// Top of the stack and remaining arguments
		int top = 0;
		Varargs v = Constants.NONE;

		// Cache some common features
		ProtoInfo info = function.info;
		Prototype prototype = function.prototype;
		int[] code = prototype.code;
		LuaValue[] constants = prototype.k;

		// Upvalues are only possible when closures create closures
		AbstractUpvalue[] upvalues = function.upvalues;
		AbstractUpvalue[] openups = prototype.p.length > 0 ? new AbstractUpvalue[stack.length] : null;

		// Create varargs "arg" table if needed
		if (prototype.is_vararg >= Lua.VARARG_NEEDSARG) stack[prototype.numparams] = new LuaTable(varargs);

		// Debug wants args to this function
		DebugHandler handler = state.debug;
		DebugState ds = handler.getDebugState();
		DebugFrame di = handler.onCall(ds, function, varargs, stack);

		// process instructions
		try {
			while (true) {
				handler.onInstruction(ds, di, pc, v, top);

				// pull out instruction
				int i = code[pc++];
				int a = ((i >> 6) & 0xff);

				// process the op code
				switch (i & 0x3f) {
					case Lua.OP_MOVE: // A B R(A):= R(B)
						stack[a] = stack[i >>> 23];
						continue;

					case Lua.OP_LOADK: // A Bx R(A):= Kst(Bx)
						stack[a] = constants[i >>> 14];
						continue;

					case Lua.OP_LOADBOOL:// A B C R(A):= (Bool)B: if (C) pc++
						stack[a] = (i >>> 23 != 0) ? Constants.TRUE : Constants.FALSE;
						if ((i & (0x1ff << 14)) != 0) {
							pc++; /* skip next instruction (if C) */
						}
						continue;

					case Lua.OP_LOADNIL: // A B R(A):= ...:= R(B):= nil
						for (int b = i >>> 23; a <= b; ) {
							stack[a++] = Constants.NIL;
						}
						continue;

					case Lua.OP_GETUPVAL: // A B R(A):= UpValue[B]
						stack[a] = upvalues[i >>> 23].getUpvalue();
						continue;

					case Lua.OP_GETGLOBAL: // A Bx R(A):= Gbl[Kst(Bx)]
						stack[a] = function.getfenv().get(state, constants[i >>> 14]);
						continue;

					case Lua.OP_GETTABLE: // A B C R(A):= R(B)[RK(C)]
					{
						int c = (i >> 14) & 0x1ff;
						int b = i >>> 23;
						stack[a] = OperationHelper.getTable(state, stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c], b);
						continue;
					}

					case Lua.OP_SETGLOBAL: // A Bx Gbl[Kst(Bx)]:= R(A)
						function.getfenv().set(state, constants[i >>> 14], stack[a]);
						continue;

					case Lua.OP_SETUPVAL: // A B UpValue[B]:= R(A)
						upvalues[i >>> 23].setUpvalue(stack[a]);
						continue;

					case Lua.OP_SETTABLE: // A B C R(A)[RK(B)]:= RK(C)
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						OperationHelper.setTable(state, stack[a], (b > 0xff ? constants[b & 0x0ff] : stack[b]), c > 0xff ? constants[c & 0x0ff] : stack[c], a);
						continue;
					}

					case Lua.OP_NEWTABLE: // A B C R(A):= {} (size = B,C)
						stack[a] = new LuaTable(i >>> 23, (i >> 14) & 0x1ff);
						continue;

					case Lua.OP_SELF: // A B C R(A+1):= R(B): R(A):= R(B)[RK(C)]
					{
						int c = (i >> 14) & 0x1ff;
						int b = i >>> 23;
						LuaValue table = stack[a + 1] = stack[i >>> 23];
						stack[a] = OperationHelper.getTable(state, table, c > 0xff ? constants[c & 0x0ff] : stack[c], b);
						continue;
					}

					case Lua.OP_ADD: // A B C R(A):= RK(B) + RK(C)
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						stack[a] = OperationHelper.add(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c], b, c);
						continue;
					}
					case Lua.OP_SUB: // A B C R(A):= RK(B) - RK(C)
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						stack[a] = OperationHelper.sub(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c], b, c);
						continue;
					}

					case Lua.OP_MUL: // A B C R(A):= RK(B) * RK(C)
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						stack[a] = OperationHelper.mul(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c], b, c);
						continue;
					}

					case Lua.OP_DIV: // A B C R(A):= RK(B) / RK(C)
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						stack[a] = OperationHelper.div(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c], b, c);
						continue;
					}

					case Lua.OP_MOD: // A B C R(A):= RK(B) % RK(C)
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						stack[a] = OperationHelper.mod(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c], b, c);
						continue;
					}

					case Lua.OP_POW: // A B C R(A):= RK(B) ^ RK(C)
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						stack[a] = OperationHelper.pow(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c], b, c);
						continue;
					}

					case Lua.OP_UNM: // A B R(A):= -R(B)
					{
						int b = i >>> 23;
						stack[a] = OperationHelper.neg(state, stack[b], b);
						continue;
					}

					case Lua.OP_NOT: // A B R(A):= not R(B)
						stack[a] = stack[i >>> 23].toBoolean() ? FALSE : TRUE;
						continue;

					case Lua.OP_LEN: // A B R(A):= length of R(B)
					{
						int b = i >>> 23;
						stack[a] = OperationHelper.length(state, stack[b], b);
						continue;
					}

					case Lua.OP_CONCAT: // A B C R(A):= R(B).. ... ..R(C)
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						int count = c - b + 1;
						if (count > 1) {
							LuaValue buffer = stack[c];
							while (--c >= b) {
								buffer = OperationHelper.concat(state, stack[c], buffer);
							}
							stack[a] = buffer;
						} else {
							stack[a] = OperationHelper.concat(state, stack[c - 1], stack[c]);
						}
					}
					continue;

					case Lua.OP_JMP: // sBx pc+=sBx
						pc += (i >>> 14) - 0x1ffff;
						continue;

					case Lua.OP_EQ: // A B C if ((RK(B) == RK(C)) ~= A) then pc++
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						if (OperationHelper.eq(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c]) == (a == 0)) {
							++pc;
						}
						continue;
					}

					case Lua.OP_LT: // A B C if ((RK(B) < RK(C)) ~= A) then pc++
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						if (OperationHelper.lt(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c]) == (a == 0)) {
							++pc;
						}
						continue;
					}

					case Lua.OP_LE: // A B C if ((RK(B) <= RK(C)) ~= A) then pc++
					{
						int b = i >>> 23;
						int c = (i >> 14) & 0x1ff;
						if (OperationHelper.le(state, b > 0xff ? constants[b & 0x0ff] : stack[b], c > 0xff ? constants[c & 0x0ff] : stack[c]) == (a == 0)) {
							++pc;
						}
						continue;
					}

					case Lua.OP_TEST: // A C if not (R(A) <=> C) then pc++
						if (stack[a].toBoolean() == ((i & (0x1ff << 14)) == 0)) {
							++pc;
						}
						continue;

					case Lua.OP_TESTSET: // A B C if (R(B) <=> C) then R(A):= R(B) else pc++
					{
						/* note: doc appears to be reversed */
						LuaValue test = stack[i >>> 23];
						if (test.toBoolean() == ((i & (0x1ff << 14)) == 0)) {
							++pc;
						} else {
							stack[a] = test; // TODO: should be sBx?
						}
						continue;
					}

					case Lua.OP_CALL: // A B C R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1))
						switch (i & (Lua.MASK_B | Lua.MASK_C)) {
							case (1 << Lua.POS_B) | (0 << Lua.POS_C):
								v = OperationHelper.invoke(state, stack[a], Constants.NONE, a);
								top = a + v.count();
								continue;
							case (2 << Lua.POS_B) | (0 << Lua.POS_C):
								v = OperationHelper.invoke(state, stack[a], stack[a + 1], a);
								top = a + v.count();
								continue;
							case (1 << Lua.POS_B) | (1 << Lua.POS_C):
								OperationHelper.call(state, stack[a], a);
								continue;
							case (2 << Lua.POS_B) | (1 << Lua.POS_C):
								OperationHelper.call(state, stack[a], stack[a + 1], a);
								continue;
							case (3 << Lua.POS_B) | (1 << Lua.POS_C):
								OperationHelper.call(state, stack[a], stack[a + 1], stack[a + 2], a);
								continue;
							case (4 << Lua.POS_B) | (1 << Lua.POS_C):
								OperationHelper.call(state, stack[a], stack[a + 1], stack[a + 2], stack[a + 3], a);
								continue;
							case (1 << Lua.POS_B) | (2 << Lua.POS_C):
								stack[a] = OperationHelper.call(state, stack[a], a);
								continue;
							case (2 << Lua.POS_B) | (2 << Lua.POS_C):
								stack[a] = OperationHelper.call(state, stack[a], stack[a + 1], a);
								continue;
							case (3 << Lua.POS_B) | (2 << Lua.POS_C):
								stack[a] = OperationHelper.call(state, stack[a], stack[a + 1], stack[a + 2], a);
								continue;
							case (4 << Lua.POS_B) | (2 << Lua.POS_C):
								stack[a] = OperationHelper.call(state, stack[a], stack[a + 1], stack[a + 2], stack[a + 3], a);
								continue;
							default: {
								int b = i >>> 23;
								int c = (i >> 14) & 0x1ff;
								v = b > 0 ?
									varargsOf(stack, a + 1, b - 1) : // exact arg count
									varargsOf(stack, a + 1, top - v.count() - (a + 1), v); // from prev top
								v = OperationHelper.invoke(state, stack[a], v, a);
								if (c > 0) {
									while (--c > 0) {
										stack[a + c - 1] = v.arg(c);
									}
									v = Constants.NONE; // TODO: necessary?
								} else {
									top = a + v.count();
								}
								continue;
							}
						}

					case Lua.OP_TAILCALL: // A B C return R(A)(R(A+1), ... ,R(A+B-1))
						switch (i & Lua.MASK_B) {
							case (1 << Lua.POS_B):
								return new TailcallVarargs(stack[a], Constants.NONE);
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
									varargsOf(stack, a + 1, top - v.count() - (a + 1), v); // from prev top
								return new TailcallVarargs(stack[a], v);
							}
						}

					case Lua.OP_RETURN: // A B return R(A), ... ,R(A+B-2) (see note)
					{
						int b = i >>> 23;
						switch (b) {
							case 0:
								return varargsOf(stack, a, top - v.count() - a, v);
							case 1:
								return Constants.NONE;
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
						LuaValue idx = OperationHelper.add(state, step, stack[a]);
						if (OperationHelper.lt(state, Constants.ZERO, step) ? OperationHelper.le(state, idx, limit) : OperationHelper.le(state, limit, idx)) {
							stack[a] = idx;
							stack[a + 3] = idx;
							pc += (i >>> 14) - 0x1ffff;
						}
					}
					continue;

					case Lua.OP_FORPREP: // A sBx R(A)-=R(A+2): pc+=sBx
					{
						LuaValue init = stack[a].checkNumber("'for' initial value must be a number");
						LuaValue limit = stack[a + 1].checkNumber("'for' limit must be a number");
						LuaValue step = stack[a + 2].checkNumber("'for' step must be a number");
						stack[a] = OperationHelper.sub(state, init, step);
						stack[a + 1] = limit;
						stack[a + 2] = step;
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
						v = OperationHelper.invoke(state, stack[a], varargsOf(stack[a + 1], stack[a + 2]), a);
						LuaValue first = v.first();
						if (first.isNil()) {
							++pc;
						} else {
							stack[a + 2] = stack[a + 3] = first;
							for (int c = (i >> 14) & 0x1ff; c > 1; --c) {
								stack[a + 2 + c] = v.arg(c);
							}
							v = Constants.NONE; // todo: necessary?
						}
						continue;
					}

					case Lua.OP_SETLIST: // A B CR(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
					{
						int c = (i >> 14) & 0x1ff;
						if (c == 0) {
							c = code[pc++];
						}
						int offset = (c - 1) * Lua.LFIELDS_PER_FLUSH;
						LuaTable table = stack[a].checkTable();
						int b = i >>> 23;
						if (b == 0) {
							b = top - a - 1;
							int m = b - v.count();
							int j = 1;
							for (; j <= m; j++) {
								table.rawset(offset + j, stack[a + j]);
							}
							for (; j <= b; j++) {
								table.rawset(offset + j, v.arg(j - m));
							}
						} else {
							table.presize(offset + b);
							for (int j = 1; j <= b; j++) {
								table.rawset(offset + j, stack[a + j]);
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
						for (int j = 0; j < nups; ++j) {
							i = code[pc++];
							//b = B(i);
							int b = i >>> 23;
							if ((i & 4) != 0) {
								newcl.upvalues[j] = upvalues[b];
							} else {
								AbstractUpvalue upvalue = openups[b];
								if (upvalue == null) {
									upvalue = openups[b] = TypeFactory.proxy(new ArrayUpvalue(stack, b));
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
							top = a + varargs.count();
							v = varargs;
						} else {
							for (int j = 1; j < b; ++j) {
								stack[a + j - 1] = varargs.arg(j);
							}
						}
					}
				}
			}
		} catch (LuaError le) {
			throw le.fillTraceback(state);
		} catch (Exception e) {
			throw new LuaError(e).fillTraceback(state);
		} finally {
			ds.onReturn();
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
