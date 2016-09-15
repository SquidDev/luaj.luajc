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
package org.squiddev.cobalt.luajc.compilation;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.cobalt.Lua;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Prototype;
import org.squiddev.cobalt.luajc.analysis.ProtoInfo;
import org.squiddev.cobalt.luajc.analysis.VarInfo;
import org.squiddev.cobalt.luajc.utils.AsmUtils;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.cobalt.Constants.*;
import static org.squiddev.cobalt.luajc.compilation.Constants.*;
import static org.squiddev.cobalt.luajc.utils.AsmUtils.constantOpcode;

public final class JavaBuilder {
	private static final int SLOT_THIS = 0;
	private static final int SLOT_STATE = 1;
	private static final int SLOT_FUNC = 2;
	private static final int SLOT_VARARGS = 3;

	public static final int BRANCH_GOTO = 1;
	public static final int BRANCH_IFNE = 2;
	public static final int BRANCH_IFEQ = 3;

	// Basic info
	private final ProtoInfo pi;
	private final Prototype p;
	private final String className;
	private final String prefix;

	/**
	 * Main class writer
	 */
	private final ClassWriter writer;

	/**
	 * The static constructor method
	 */
	private final MethodVisitor init;

	/**
	 * The function invoke
	 */
	private final MethodVisitor main;

	/**
	 * Max number of locals
	 */
	private int maxLocals;

	/**
	 * The local index of the varargs result
	 */
	private int varargsLocal = -1;

	// Labels for locals
	private final Label start = new Label();
	private final Label end = new Label();
	private final Label exceptionHandler = new Label();
	private final Label errorHandler = new Label();

	// the superclass arg count, 0-3 args, 4=varargs
	private final FunctionType superclass;

	/**
	 * Go to destinations
	 */
	private final Label[] branchDestinations;

	/**
	 * Slot for {@link org.squiddev.cobalt.debug.DebugHandler}
	 */
	private final int debugHandlerSlot;

	/**
	 * Slot for {@link org.squiddev.cobalt.debug.DebugFrame}
	 */
	private final int debugInfoSlot;

	/**
	 * Slots for {@link org.squiddev.cobalt.debug.DebugState}
	 */
	private final int debugStateSlot;

	/**
	 * Slot the upvalues live in
	 */
	private final int upvaluesSlot;

	private int line = 0;

	private final Map<LuaValue, String> constants = new HashMap<LuaValue, String>();

	private final SlotInfo[] slots;

	public JavaBuilder(ProtoInfo pi, String prefix, String filename) {
		this.pi = pi;
		this.p = pi.prototype;
		this.prefix = prefix;
		this.slots = new SlotInfo[p.maxstacksize];

		String className = this.className = prefix + pi.name;

		// what class to inherit from
		int superclassType = p.numparams;
		if (p.is_vararg != 0 || superclassType >= SUPERTYPE_VARARGS_ID) {
			superclassType = SUPERTYPE_VARARGS_ID;
		}

		// If we return var args, then must be a var arg function
		for (int i = 0, n = p.code.length; i < n; i++) {
			int inst = p.code[i];
			int o = Lua.GET_OPCODE(inst);
			if ((o == Lua.OP_TAILCALL) || ((o == Lua.OP_RETURN) && (Lua.GETARG_B(inst) < 1 || Lua.GETARG_B(inst) > 2))) {
				superclassType = SUPERTYPE_VARARGS_ID;
				break;
			}
		}

		FunctionType superType = superclass = SUPER_TYPES[superclassType];
		maxLocals = superType.argsLength;

		// Create class writer
		// We don't need to compute frames as slots do not change their types
		writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		writer.visit(V1_6, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, className, null, superType.className, null);
		writer.visitSource(filename, null);
		AsmUtils.writeDefaultConstructor(writer, superType.className);

		// Create the class constructor (used for constants)
		init = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		init.visitCode();

		// Create the invoke method
		main = writer.visitMethod(ACC_PUBLIC | ACC_FINAL, EXECUTE_NAME, superType.signature, null, null);
		main.visitCode();

		// Setup debug slots
		debugHandlerSlot = ++maxLocals;
		debugStateSlot = ++maxLocals;
		debugInfoSlot = ++maxLocals;

		// On method call, store callstack in slot
		main.visitVarInsn(ALOAD, SLOT_STATE);
		main.visitFieldInsn(GETFIELD, CLASS_STATE, "debug", TYPE_HANDLER);
		main.visitVarInsn(ASTORE, debugHandlerSlot);

		main.visitVarInsn(ALOAD, debugHandlerSlot);
		METHOD_GETSTATE.inject(main);
		main.visitVarInsn(ASTORE, debugStateSlot);

		main.visitVarInsn(ALOAD, debugHandlerSlot);
		main.visitVarInsn(ALOAD, debugStateSlot);
		main.visitVarInsn(ALOAD, SLOT_FUNC);
		loadNone();
		main.visitInsn(ACONST_NULL); // TODO: Patch Cobalt so this won't break
		METHOD_ONCALL.inject(main);

		main.visitVarInsn(ASTORE, debugInfoSlot);

		if (p.nups > 0) {
			upvaluesSlot = ++maxLocals;
			main.visitVarInsn(ALOAD, SLOT_FUNC);
			upvaluesGet();
			main.visitVarInsn(ASTORE, upvaluesSlot);
		} else {
			upvaluesSlot = -1;
		}

		// Initialize the values in the slots
		initializeSlots();

		// Beginning for variable names
		// Also for try catch block
		main.visitTryCatchBlock(start, end, errorHandler, "org/squiddev/cobalt/LuaError");
		main.visitTryCatchBlock(start, end, exceptionHandler, "java/lang/Exception");
		main.visitTryCatchBlock(start, end, end, null);
		main.visitLabel(start);

		{
			// Generate a label for every instruction
			int nc = p.code.length;
			Label[] branchDestinations = this.branchDestinations = new Label[nc];
			for (int pc = 0; pc < nc; pc++) {
				branchDestinations[pc] = new Label();
			}
		}
	}

	/**
	 * Setup slots for arguments
	 */
	public void initializeSlots() {
		int slot;
		createUpvalues(-1, 0, p.maxstacksize);

		if (superclass == SUPERTYPE_VARARGS) {
			for (slot = 0; slot < p.numparams; slot++) {
				if (pi.params[slot].isReferenced) {
					main.visitVarInsn(ALOAD, SLOT_VARARGS);
					constantOpcode(main, slot + 1);
					METHOD_VARARGS_ARG.inject(main, INVOKEVIRTUAL);
					storeLocal(-1, slot);
				}
			}
			boolean needsArg = ((p.is_vararg & Lua.VARARG_NEEDSARG) != 0);
			if (needsArg) {
				main.visitVarInsn(ALOAD, SLOT_VARARGS);
				constantOpcode(main, p.numparams + 1);
				METHOD_TABLEOF.inject(main, INVOKESTATIC);
				storeLocal(-1, slot++);
			} else if (p.numparams > 0) {
				main.visitVarInsn(ALOAD, SLOT_VARARGS);
				constantOpcode(main, p.numparams + 1);
				METHOD_VARARGS_SUBARGS.inject(main, INVOKEVIRTUAL);
				main.visitVarInsn(ASTORE, SLOT_VARARGS);
			}
		} else {
			// fixed arg function between 0 and 3 arguments
			for (slot = 0; slot < p.numparams; slot++) {
				SlotInfo info = slots[slot] = new SlotInfo(slot);
				info.valueSlot = slot + SLOT_VARARGS;
				if (pi.params[slot].isUpvalueCreate(-1)) {
					main.visitVarInsn(ALOAD, slot + SLOT_VARARGS);
					storeLocal(-1, slot);
				}
			}
		}

		// nil parameters
		for (; slot < p.maxstacksize; slot++) {
			if (pi.params[slot].isReferenced) {
				loadNil();
				storeLocal(-1, slot);
			}
		}
	}

	public byte[] completeClass() {
		// Finish class initializer
		init.visitInsn(RETURN);
		init.visitMaxs(0, 0);
		init.visitEnd();

		// Finish main function

		// On throwable
		main.visitLabel(end);
		main.visitVarInsn(ALOAD, debugHandlerSlot);
		main.visitVarInsn(ALOAD, debugStateSlot);
		METHOD_ONRETURN.inject(main);
		main.visitInsn(ATHROW);

		// On normal exception
		main.visitLabel(exceptionHandler);
		METHOD_WRAP_ERROR.inject(main);
		main.visitLabel(errorHandler);
		loadState();
		METHOD_FILL_TRACEBACK.inject(main);
		main.visitVarInsn(ALOAD, debugHandlerSlot);
		main.visitVarInsn(ALOAD, debugStateSlot);
		METHOD_ONRETURN.inject(main);
		main.visitInsn(ATHROW);

		main.visitMaxs(0, 0);

		// Add upvalue & local value slot names
		for (SlotInfo slot : slots) {
			if (slot != null) slot.injectSlot(main, start, end);
		}

		main.visitEnd();

		writer.visitEnd();

		return writer.toByteArray();
	}

	public void dup() {
		main.visitInsn(DUP);
	}

	public void pop() {
		main.visitInsn(POP);
	}

	public void swap() {
		main.visitInsn(SWAP);
	}

	public void loadNil() {
		main.visitFieldInsn(GETSTATIC, "org/squiddev/cobalt/Constants", "NIL", "Lorg/squiddev/cobalt/LuaValue;");
	}

	public void loadNone() {
		main.visitFieldInsn(GETSTATIC, "org/squiddev/cobalt/Constants", "NONE", "Lorg/squiddev/cobalt/LuaValue;");
	}

	public void loadBoolean(boolean b) {
		main.visitFieldInsn(GETSTATIC, "org/squiddev/cobalt/Constants", b ? "TRUE" : "FALSE", "Lorg/squiddev/cobalt/LuaBoolean;");
	}

	private int findSlotIndex(int slot, boolean isUpvalue) {
		SlotInfo info = slots[slot];
		if (info == null) info = slots[slot] = new SlotInfo(slot);

		if (isUpvalue) {
			int javaSlot = info.upvalueSlot;
			if (javaSlot < 0) javaSlot = info.upvalueSlot = ++maxLocals;
			return javaSlot;
		} else {
			int javaSlot = info.valueSlot;
			if (javaSlot < 0) javaSlot = info.valueSlot = ++maxLocals;
			return javaSlot;
		}
	}

	public void loadLocal(int pc, int slot) {
		boolean isUpvalue = pi.getVariable(pc, slot).isUpvalueRefer();
		int index = findSlotIndex(slot, isUpvalue);

		main.visitVarInsn(ALOAD, index);
		if (isUpvalue) {
			Constants.METHOD_GET_UPVALUE.inject(main);
		}
	}

	public void storeLocal(int pc, int slot) {
		VarInfo var = pc < 0 ? pi.params[slot] : pi.vars[pc][slot];
		boolean isUpvalue = var.isUpvalueAssign();
		int index = findSlotIndex(slot, isUpvalue);
		if (isUpvalue) {
			boolean isUpCreate = var.isUpvalueCreate(pc);
			if (isUpCreate) {
				// If we are creating the upvalue for the first time then we call LibFunction.emptyUpvalue (but actually call
				// <className>.emptyUpvalue but I need to check that). The we duplicate the object, so it remains on the stack
				// and store it
				METHOD_NEW_UPVALUE_EMPTY.inject(main);

				// We should only proxy when we need to switch back into interpreted mode
				// and this upvalue will be mutated again
				main.visitInsn(DUP);
				main.visitVarInsn(ASTORE, index);
			} else {
				main.visitVarInsn(ALOAD, index);
			}

			// We swap the values which is the value and the reference
			// And store to the reference
			main.visitInsn(SWAP);
			Constants.METHOD_SET_UPVALUE.inject(main);
		} else {
			main.visitVarInsn(ASTORE, index);
		}
	}

	public void createUpvalues(int pc, int firstSlot, int numSlots) {
		for (int i = 0; i < numSlots; i++) {
			int slot = firstSlot + i;
			if (pi.getVariable(pc, slot).isUpvalueCreate(pc)) {
				int index = findSlotIndex(slot, true);
				METHOD_NEW_UPVALUE_NIL.inject(main);
				main.visitVarInsn(ASTORE, index);
			}
		}
	}

	public void convertToUpvalue(int pc, int slot) {
		boolean isUpvalueAssign = pi.vars[pc][slot].isUpvalueAssign();
		if (isUpvalueAssign) {
			int index = findSlotIndex(slot, false);

			// Load it from the slot, convert to an array and store it to the upvalue slot
			main.visitVarInsn(ALOAD, index);
			METHOD_NEW_UPVALUE_VALUE.inject(main);
			int upvalueIndex = findSlotIndex(slot, true);
			main.visitVarInsn(ASTORE, upvalueIndex);
		}
	}

	public void loadUpvalue(int upvalueIndex) {
		main.visitVarInsn(ALOAD, upvaluesSlot);
		constantOpcode(main, upvalueIndex);
		main.visitInsn(AALOAD);
		METHOD_GET_UPVALUE.inject(main);
	}

	public void storeUpvalue(int pc, int upvalueIndex, int slot) {
		main.visitVarInsn(ALOAD, upvaluesSlot);
		constantOpcode(main, upvalueIndex);
		main.visitInsn(AALOAD);

		loadLocal(pc, slot);
		Constants.METHOD_SET_UPVALUE.inject(main);
	}

	public void newTable(int b, int c) {
		constantOpcode(main, b);
		constantOpcode(main, c);
		METHOD_TABLEOF_DIMS.inject(main);
	}

	public void loadState() {
		main.visitVarInsn(ALOAD, SLOT_STATE);
	}

	public void loadEnv() {
		main.visitVarInsn(ALOAD, SLOT_FUNC);
		METHOD_GETENV.inject(main);
	}

	public void loadVarargs() {
		main.visitVarInsn(ALOAD, SLOT_VARARGS);
	}

	public void loadVarargs(int index) {
		loadVarargs();
		arg(index);
	}

	public void arg(int index) {
		if (index == 1) {
			METHOD_VARARGS_ARG1.inject(main);
		} else {
			constantOpcode(main, index);
			METHOD_VARARGS_ARG.inject(main);
		}
	}

	private int getVarResultIndex() {
		if (varargsLocal < 0) varargsLocal = ++maxLocals;
		return varargsLocal;
	}

	public void loadVarResult() {
		main.visitVarInsn(ALOAD, getVarResultIndex());
	}

	public void storeVarResult() {
		main.visitVarInsn(ASTORE, getVarResultIndex());
	}

	public void subArgs(int first) {
		constantOpcode(main, first);
		METHOD_VARARGS_SUBARGS.inject(main);
	}

	public void getTable(int slot) {
		constantOpcode(main, slot);
		METHOD_TABLE_GET.inject(main);
	}

	public void setTable(int tSlot) {
		constantOpcode(main, tSlot);
		METHOD_TABLE_SET.inject(main);
	}

	public void opNot() {
		main.visitMethodInsn(INVOKEVIRTUAL, CLASS_LUAVALUE, "toBoolean", "()Z", false);
		main.visitInsn(ICONST_0);

		Label cont = new Label(), loadTrue = new Label();
		main.visitJumpInsn(IF_ICMPEQ, loadTrue);

		main.visitFieldInsn(GETSTATIC, CLASS_CONSTANTS, "FALSE", TYPE_LUABOOLEAN);
		main.visitJumpInsn(GOTO, cont);

		main.visitLabel(loadTrue);
		main.visitFieldInsn(GETSTATIC, CLASS_CONSTANTS, "TRUE", TYPE_LUABOOLEAN);

		main.visitLabel(cont);
	}

	public void unaryOp(int o, int slot) {
		String op;
		switch (o) {
			default:
			case Lua.OP_UNM:
				op = "neg";
				break;
			case Lua.OP_LEN:
				op = "length";
				break;
			case Lua.OP_NOT:
				op = "not";
				break;
		}

		constantOpcode(main, slot);
		main.visitMethodInsn(INVOKESTATIC, CLASS_OPERATION, op, "(" + TYPE_STATE + TYPE_LUAVALUE + "I)" + TYPE_LUAVALUE, false);
	}

	public void binaryOp(int o, int b, int c) {
		String op;
		switch (o) {
			default:
			case Lua.OP_ADD:
				op = "add";
				break;
			case Lua.OP_SUB:
				op = "sub";
				break;
			case Lua.OP_MUL:
				op = "mul";
				break;
			case Lua.OP_DIV:
				op = "div";
				break;
			case Lua.OP_MOD:
				op = "mod";
				break;
			case Lua.OP_POW:
				op = "pow";
				break;
		}

		constantOpcode(main, b);
		constantOpcode(main, c);
		main.visitMethodInsn(INVOKESTATIC, CLASS_OPERATION, op, "(" + TYPE_STATE + TYPE_LUAVALUE + TYPE_LUAVALUE + "II)" + TYPE_LUAVALUE, false);
	}

	public void compareOp(int o) {
		String op;
		switch (o) {
			default:
			case Lua.OP_EQ:
				op = "eq";
				break;
			case Lua.OP_LT:
				op = "lt";
				break;
			case Lua.OP_LE:
				op = "le";
				break;
		}
		main.visitMethodInsn(INVOKESTATIC, CLASS_OPERATION, op, "(" + TYPE_STATE + TYPE_LUAVALUE + TYPE_LUAVALUE + ")Z", false);
	}

	public void visitReturn() {
		// Pop call stack
		main.visitVarInsn(ALOAD, debugHandlerSlot);
		main.visitVarInsn(ALOAD, debugStateSlot);
		METHOD_ONRETURN.inject(main);

		main.visitInsn(ARETURN);
	}

	public void visitToBoolean() {
		METHOD_VALUE_TO_BOOL.inject(main);
	}

	public void visitIsNil() {
		METHOD_IS_NIL.inject(main);
	}

	public void testForLoop() {
		loadConstant(org.squiddev.cobalt.Constants.ZERO);
		compareOp(Lua.OP_LE);
		main.visitInsn(ICONST_0);

		Label lt = new Label();
		main.visitJumpInsn(IF_ICMPEQ, lt);
		swap();
		main.visitLabel(lt);
		compareOp(Lua.OP_LE);
	}

	public void loadArrayArgs(int pc, int firstSlot, int nargs) {
		constantOpcode(main, nargs);
		main.visitTypeInsn(ANEWARRAY, CLASS_LUAVALUE);
		for (int i = 0; i < nargs; i++) {
			main.visitInsn(DUP);
			constantOpcode(main, i);
			loadLocal(pc, firstSlot++);
			main.visitInsn(AASTORE);
		}
	}

	public void newVarargs(int pc, int firstslot, int nargs) {
		switch (nargs) {
			case 0:
				loadNone();
				break;
			case 1:
				loadLocal(pc, firstslot);
				break;
			case 2:
				loadLocal(pc, firstslot);
				loadLocal(pc, firstslot + 1);
				METHOD_VARARGS_ONE.inject(main);
				break;
			case 3:
				loadLocal(pc, firstslot);
				loadLocal(pc, firstslot + 1);
				loadLocal(pc, firstslot + 2);
				METHOD_VARARGS_TWO.inject(main);
				break;
			default:
				loadArrayArgs(pc, firstslot, nargs);
				METHOD_VARARGS_MANY.inject(main);
				break;
		}
	}

	public void newVarargsVarResult(int pc, int firstSlots, int slotCount) {
		loadArrayArgs(pc, firstSlots, slotCount);
		loadVarResult();
		METHOD_VARARGS_MANY_VAR.inject(main);
	}

	public void call(int nargs, int slot) {
		constantOpcode(main, slot);
		switch (nargs) {
			case 0:
				METHOD_CALL_NONE.inject(main);
				break;
			case 1:
				METHOD_CALL_ONE.inject(main);
				break;
			case 2:
				METHOD_CALL_TWO.inject(main);
				break;
			case 3:
				METHOD_CALL_THREE.inject(main);
				break;
			default:
				throw new IllegalArgumentException("can't call with " + nargs + " args");
		}
	}

	public void newTailcallVarargs() {
		METHOD_TAILCALL.inject(main);
	}

	public void invoke(int nargs, int slot) {
		constantOpcode(main, slot);
		switch (nargs) {
			case -1:
				break;
			case 0:
				loadNone();
				break;
			case 1:
				// It is only one item so we can call it with a varargs
				break;
			case 2:
				METHOD_VARARGS_ONE.inject(main);
				break;
			case 3:
				METHOD_VARARGS_TWO.inject(main);
				break;
			default:
				throw new IllegalArgumentException("can't invoke with " + nargs + " args");
		}

		METHOD_INVOKE_VAR.inject(main);
	}

	public void closureCreate(ProtoInfo info) {
		main.visitTypeInsn(NEW, CLASS_WRAPPER);
		main.visitInsn(DUP);
		main.visitFieldInsn(GETSTATIC, prefix + PROTOTYPE_STORAGE, PROTOTYPE_NAME + info.name, TYPE_PROTOINFO);
		loadEnv();
		main.visitMethodInsn(INVOKESPECIAL, CLASS_WRAPPER, "<init>", "(" + TYPE_PROTOINFO + TYPE_LUATABLE + ")V", false);
	}

	public void upvaluesGet() {
		main.visitFieldInsn(GETFIELD, CLASS_WRAPPER, "upvalues", "[" + TYPE_UPVALUE);
	}

	public void initUpvalueFromUpvalue(int newUpvalue, int upvalueIndex) {
		constantOpcode(main, newUpvalue);
		main.visitVarInsn(ALOAD, upvaluesSlot);
		constantOpcode(main, upvalueIndex);
		main.visitInsn(AALOAD);
		main.visitInsn(AASTORE);
	}

	public void initUpvalueFromLocal(int newUpvalue, int pc, int srcSlot) {
		boolean isReadWrite = pi.vars[pc][srcSlot].upvalue.readWrite;
		int index = findSlotIndex(srcSlot, isReadWrite);

		constantOpcode(main, newUpvalue);
		main.visitVarInsn(ALOAD, index);
		if (!isReadWrite) METHOD_NEW_UPVALUE_VALUE.inject(main);
		main.visitInsn(AASTORE);
	}

	public void loadConstant(LuaValue value) {
		switch (value.type()) {
			case TNIL:
				loadNil();
				break;
			case TBOOLEAN:
				loadBoolean(value.toBoolean());
				break;
			case TNUMBER:
			case TSTRING:
				String name = constants.get(value);
				if (name == null) {
					if (value.type() == TNUMBER) {
						name = value.isIntExact() ? createLuaIntegerField(value.checkInteger()) : createLuaDoubleField(value.checkDouble());
					} else {
						name = createLuaStringField(value.checkLuaString());
					}

					constants.put(value, name);
				}
				main.visitFieldInsn(GETSTATIC, className, name, TYPE_LUAVALUE);
				break;
			default:
				throw new IllegalArgumentException("bad constant type: " + value.type());
		}
	}

	private String createLuaIntegerField(int value) {
		String name = PREFIX_CONSTANT + constants.size();
		writer.visitField(ACC_STATIC | ACC_FINAL, name, TYPE_LUAVALUE, null, null);

		constantOpcode(init, value);
		METHOD_VALUEOF_INT.inject(init);
		init.visitFieldInsn(PUTSTATIC, className, name, TYPE_LUAVALUE);
		return name;
	}

	private String createLuaDoubleField(double value) {
		String name = PREFIX_CONSTANT + constants.size();
		writer.visitField(ACC_STATIC | ACC_FINAL, name, TYPE_LUAVALUE, null, null);
		constantOpcode(init, value);
		METHOD_VALUEOF_DOUBLE.inject(init);
		init.visitFieldInsn(PUTSTATIC, className, name, TYPE_LUAVALUE);
		return name;
	}

	private String createLuaStringField(LuaString value) {
		String name = PREFIX_CONSTANT + constants.size();
		writer.visitField(ACC_STATIC | ACC_FINAL, name, TYPE_LUAVALUE, null, null);

		LuaString ls = value.checkLuaString();

		constantOpcode(init, ls.length);
		init.visitIntInsn(NEWARRAY, T_BYTE);
		for (int j = 0; j < ls.length; j++) {
			init.visitInsn(DUP);
			constantOpcode(init, j);
			constantOpcode(init, ls.luaByte(j));
			init.visitInsn(BASTORE);
		}
		METHOD_VALUEOF_BYTEARRAY.inject(init);
		init.visitFieldInsn(PUTSTATIC, className, name, TYPE_LUAVALUE);
		return name;
	}

	public void addBranch(int branchType, int targetPc) {
		int type;
		switch (branchType) {
			default:
			case BRANCH_GOTO:
				type = GOTO;
				break;
			case BRANCH_IFNE:
				type = IFNE;
				break;
			case BRANCH_IFEQ:
				type = IFEQ;
				break;
		}

		main.visitJumpInsn(type, branchDestinations[targetPc]);
	}

	/**
	 * This is a really ugly way of generating the branch instruction.
	 * Every Lua instruction is assigned one label, so jumping is possible.
	 *
	 * @param pc The current Lua program counter
	 */
	public void onStartOfLuaInstruction(int pc) {
		Label currentLabel = branchDestinations[pc];

		main.visitLabel(currentLabel);

		if (p.lineinfo != null && p.lineinfo.length > pc) {
			int newLine = p.lineinfo[pc];
			if (newLine != line) {
				line = newLine;
				main.visitLineNumber(line, currentLabel);
			}
		}

		main.visitVarInsn(ALOAD, debugHandlerSlot);
		main.visitVarInsn(ALOAD, debugStateSlot);
		main.visitVarInsn(ALOAD, debugInfoSlot);
		constantOpcode(main, pc);
		main.visitInsn(ACONST_NULL);
		main.visitInsn(ICONST_M1);
		METHOD_BYTECODE.inject(main);
	}

	public void visitSetlistStack(int pc, int a0, int index0, int nvals) {
		for (int i = 0; i < nvals; i++) {
			main.visitInsn(DUP);
			constantOpcode(main, index0 + i);
			loadLocal(pc, a0 + i);
			METHOD_RAWSET.inject(main);
		}
	}

	public void visitSetlistVarargs(int index) {
		loadVarResult();
		constantOpcode(main, index);
		METHOD_RAWSET_LIST.inject(main);
	}

	public void visitConcatValue() {
		METHOD_STRING_CONCAT.inject(main);
	}
}
