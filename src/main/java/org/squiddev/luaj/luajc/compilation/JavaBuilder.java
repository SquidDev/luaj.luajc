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

import org.luaj.vm2.*;
import org.luaj.vm2.lib.DebugLib;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.luajc.Constants;
import org.squiddev.luaj.luajc.analysis.LivenessTracker;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.VarInfo;
import org.squiddev.luaj.luajc.utils.AsmUtils;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.luajc.Constants.*;
import static org.squiddev.luaj.luajc.utils.AsmUtils.constantOpcode;

public final class JavaBuilder {
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
	private final Label start;
	private final Label end;

	// the superclass arg count, 0-3 args, 4=varargs
	private final FunctionType superclass;

	/**
	 * Go to destinations
	 */
	private final Label[] branchDestinations;

	/**
	 * The slot for the {@link LuaThread.CallStack}
	 */
	private final int callStackSlot;

	/**
	 * Slot for {@link org.luaj.vm2.lib.DebugLib.DebugInfo}
	 */
	private final int debugInfoSlot;

	/**
	 * Slots for {@link org.luaj.vm2.lib.DebugLib.DebugState}
	 */
	private final int debugStateSlot;

	/**
	 * Slot the upvalues live in
	 */
	private final int upvaluesSlot;

	/**
	 * Slot to store the variable stack in
	 */
	private int stackSlot = -1;

	/**
	 * Slot to store open upvalues in
	 */
	private int openupsSlot = -1;

	private int line = 0;

	/**
	 * Tracker for variable livelyness
	 */
	private final LivenessTracker tracker;

	/**
	 * Lookup for constant to field names
	 */
	private final Map<LuaValue, String> constants = new HashMap<LuaValue, String>();

	public JavaBuilder(ProtoInfo pi, String prefix, String filename) {
		this.pi = pi;
		this.p = pi.prototype;
		this.prefix = prefix;
		this.tracker = new LivenessTracker(pi);
		this.slots = new SlotInfo[pi.prototype.maxstacksize];

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

		// Beginning and end of methods for visitors
		start = new Label();
		end = new Label();
		main.visitLabel(start);

		// On method call, store callstack in slot
		callStackSlot = ++maxLocals;
		main.visitVarInsn(ALOAD, 1);
		METHOD_ONCALL.inject(main);
		main.visitVarInsn(ASTORE, callStackSlot);

		if (DebugLib.DEBUG_ENABLED) {
			debugStateSlot = ++maxLocals;
			debugInfoSlot = ++maxLocals;

			METHOD_GETSTATE.inject(main);
			main.visitInsn(DUP);
			main.visitVarInsn(ASTORE, debugStateSlot);
			METHOD_GETINFO.inject(main);
			main.visitVarInsn(ASTORE, debugInfoSlot);
		} else {
			debugStateSlot = -1;
			debugInfoSlot = -1;
		}

		if (p.nups > 0) {
			upvaluesSlot = ++maxLocals;
			main.visitVarInsn(ALOAD, 1);
			upvaluesGet();
			main.visitVarInsn(ASTORE, upvaluesSlot);
		} else {
			upvaluesSlot = -1;
		}

		// Initialize the values in the slots
		initializeSlots();

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
					main.visitVarInsn(ALOAD, VARARGS_SLOT);
					constantOpcode(main, slot + 1);
					METHOD_VARARGS_ARG.inject(main, INVOKEVIRTUAL);
					storeLocal(-1, slot);
				}
			}
			boolean needsArg = ((p.is_vararg & Lua.VARARG_NEEDSARG) != 0);
			if (needsArg) {
				main.visitVarInsn(ALOAD, VARARGS_SLOT);
				constantOpcode(main, p.numparams + 1);
				METHOD_TABLEOF.inject(main, INVOKESTATIC);
				storeLocal(-1, slot++);
			} else if (p.numparams > 0) {
				main.visitVarInsn(ALOAD, VARARGS_SLOT);
				constantOpcode(main, p.numparams + 1);
				METHOD_VARARGS_SUBARGS.inject(main, INVOKEVIRTUAL);
				main.visitVarInsn(ASTORE, VARARGS_SLOT);
			}
		} else {
			// fixed arg function between 0 and 3 arguments
			for (slot = 0; slot < p.numparams; slot++) {
				findSlot(slot).valueSlot = slot + VARARGS_SLOT;
				if (pi.isUpvalueCreate(-1, slot)) {
					main.visitVarInsn(ALOAD, slot + VARARGS_SLOT);
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
		main.visitLabel(end);
		main.visitMaxs(0, 0);

		// Add upvalue & local value slot names
		for (SlotInfo slot : slots) {
			if (slot != null) slot.visitLocals(main, start, end);
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

	public void loadNil() {
		main.visitFieldInsn(GETSTATIC, "org/luaj/vm2/LuaValue", "NIL", "Lorg/luaj/vm2/LuaValue;");
	}

	public void loadNone() {
		main.visitFieldInsn(GETSTATIC, "org/luaj/vm2/LuaValue", "NONE", "Lorg/luaj/vm2/LuaValue;");
	}

	public void loadBoolean(boolean b) {
		String field = (b ? "TRUE" : "FALSE");
		main.visitFieldInsn(GETSTATIC, "org/luaj/vm2/LuaValue", field, "Lorg/luaj/vm2/LuaBoolean;");
	}

	private final SlotInfo[] slots;

	private SlotInfo findSlot(int luaSlot) {
		SlotInfo info = slots[luaSlot];
		if (info == null) info = slots[luaSlot] = new SlotInfo(luaSlot);

		return info;
	}

	private int findSlotIndex(int slot, boolean isUpvalue) {
		SlotInfo info = findSlot(slot);
		if (isUpvalue) {
			int index = info.upvalueSlot;
			if (index < 0) index = info.upvalueSlot = ++maxLocals;
			return index;
		} else {
			int index = info.valueSlot;
			if (index < 0) index = info.valueSlot = ++maxLocals;
			return index;
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
		boolean isUpvalue = pi.isUpvalueAssign(pc, slot);
		int index = findSlotIndex(slot, isUpvalue);
		if (isUpvalue) {
			boolean isUpCreate = pi.isUpvalueCreate(pc, slot);
			if (isUpCreate) {
				// If we are creating the upvalue for the first time then we call LibFunction.emptyUpvalue (but actually call
				// <className>.emptyUpvalue but I need to check that). The we duplicate the object, so it remains on the stack
				// and store it
				METHOD_NEW_UPVALUE_EMPTY.inject(main);

				// We should only proxy when we need to switch back into interpreted mode
				// and this upvalue will be mutated again
				METHOD_NEW_UPVALUE_PROXY.inject(main);
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
			if (pi.isUpvalueCreate(pc, slot)) {
				int index = findSlotIndex(slot, true);
				METHOD_NEW_UPVALUE_NIL.inject(main);
				METHOD_NEW_UPVALUE_PROXY.inject(main);
				main.visitVarInsn(ASTORE, index);
			}
		}
	}

	public void convertToUpvalue(int pc, int slot) {
		boolean isUpvalueAssign = pi.isUpvalueAssign(pc, slot);
		if (isUpvalueAssign) {
			int index = findSlotIndex(slot, false);

			// Load it from the slot, convert to an array and store it to the upvalue slot
			main.visitVarInsn(ALOAD, index);
			METHOD_NEW_UPVALUE_VALUE.inject(main);
			METHOD_NEW_UPVALUE_PROXY.inject(main);
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

	public void loadEnv() {
		main.visitVarInsn(ALOAD, 1);
		METHOD_GETENV.inject(main);
	}

	public void loadVarargs() {
		main.visitVarInsn(ALOAD, VARARGS_SLOT);
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

	public void getTable() {
		METHOD_TABLE_GET.inject(main);
	}

	public void setTable() {
		METHOD_TABLE_SET.inject(main);
	}

	public void unaryOp(int o) {
		String op;
		switch (o) {
			default:
			case Lua.OP_UNM:
				op = "min";
				break;
			case Lua.OP_NOT:
				op = "not";
				break;
			case Lua.OP_LEN:
				op = "len";
				break;
		}

		main.visitMethodInsn(INVOKEVIRTUAL, CLASS_LUAVALUE, op, "()" + TYPE_LUAVALUE, false);
	}

	public void binaryOp(int o) {
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
		main.visitMethodInsn(INVOKEVIRTUAL, CLASS_LUAVALUE, op, "(" + TYPE_LUAVALUE + ")" + TYPE_LUAVALUE, false);
	}

	public void compareOp(int o) {
		String op;
		switch (o) {
			default:
			case Lua.OP_EQ:
				op = "eq_b";
				break;
			case Lua.OP_LT:
				op = "lt_b";
				break;
			case Lua.OP_LE:
				op = "lteq_b";
				break;
		}
		main.visitMethodInsn(INVOKEVIRTUAL, CLASS_LUAVALUE, op, "(" + TYPE_LUAVALUE + ")Z", false);
	}

	public void visitReturn() {
		// Pop call stack
		main.visitVarInsn(ALOAD, callStackSlot);
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
		METHOD_TESTFOR_B.inject(main);
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

	public void call(int nargs) {
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

	public void invoke(int nargs) {
		switch (nargs) {
			case -1:
				METHOD_INVOKE_VAR.inject(main);
				break;
			case 0:
				METHOD_INVOKE_NONE.inject(main);
				break;
			case 1:
				METHOD_INVOKE_VAR.inject(main); // It is only one item so we can call it with a varargs
				break;
			case 2:
				METHOD_INVOKE_TWO.inject(main);
				break;
			case 3:
				METHOD_INVOKE_THREE.inject(main);
				break;
			default:
				throw new IllegalArgumentException("can't invoke with " + nargs + " args");
		}
	}

	public void closureCreate(ProtoInfo info) {
		main.visitTypeInsn(NEW, CLASS_WRAPPER);
		main.visitInsn(DUP);
		main.visitFieldInsn(GETSTATIC, prefix + PROTOTYPE_STORAGE, PROTOTYPE_NAME + info.name, TYPE_PROTOINFO);
		loadEnv();
		main.visitMethodInsn(INVOKESPECIAL, CLASS_WRAPPER, "<init>", "(" + TYPE_PROTOINFO + TYPE_LUAVALUE + ")V", false);
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
			case LuaValue.TNIL:
				loadNil();
				break;
			case LuaValue.TBOOLEAN:
				loadBoolean(value.toboolean());
				break;
			case LuaValue.TNUMBER:
			case LuaValue.TSTRING:
				String name = constants.get(value);
				if (name == null) {
					if (value.type() == LuaValue.TNUMBER) {
						name = value.isinttype() ? createLuaIntegerField(value.checkint()) : createLuaDoubleField(value.checkdouble());
					} else {
						name = createLuaStringField(value.checkstring());
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

		LuaString ls = value.checkstring();
		if (ls.isValidUtf8()) {
			init.visitLdcInsn(value.tojstring());
			METHOD_VALUEOF_STRING.inject(init);
		} else {
			char[] c = new char[ls.m_length];
			for (int j = 0; j < ls.m_length; j++) {
				c[j] = (char) (0xff & (int) (ls.m_bytes[ls.m_offset + j]));
			}
			init.visitLdcInsn(new String(c));
			METHOD_TO_CHARARRAY.inject(init);
			METHOD_VALUEOF_CHARARRAY.inject(init);
		}
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
	 * If debugging is enabled, then this will call {@link DebugLib#debugBytecode(int, Varargs, int)}.
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

		if (DebugLib.DEBUG_ENABLED) {
			main.visitVarInsn(ALOAD, debugStateSlot);
			main.visitVarInsn(ALOAD, debugInfoSlot);
			constantOpcode(main, pc);
			main.visitInsn(ACONST_NULL);
			main.visitInsn(ICONST_M1);
			METHOD_BYTECODE.inject(main);
		}
	}

	public void visitResume(int pc, int top) {
		// The main stack
		int stackS = stackSlot;
		if (stackS == -1) stackS = stackSlot = ++maxLocals;
		constantOpcode(main, p.maxstacksize);
		main.visitTypeInsn(ANEWARRAY, TYPE_LUAVALUE);
		main.visitVarInsn(ASTORE, stackS);

		// Store the open upvalues
		boolean upvalues = p.p.length > 0;
		int openupsS = openupsSlot;
		if (upvalues) {
			if (openupsS == -1) openupsS = openupsSlot = ++maxLocals;
			constantOpcode(main, p.maxstacksize);
			main.visitTypeInsn(ANEWARRAY, TYPE_UPVALUE);
			main.visitVarInsn(ASTORE, stackS);
		}

		// Setup upvalues and stack
		VarInfo[] vars = pi.vars[pc];
		for (int slot = 0; slot < p.maxstacksize; slot++) {
			main.visitVarInsn(ALOAD, stackS);

			VarInfo info = vars[slot];
			constantOpcode(main, slot);
			if (tracker.isLive(info, pc)) {
				boolean isUpvalue = pi.getVariable(pc, slot).isUpvalueRefer();
				int index = findSlotIndex(slot, isUpvalue);

				main.visitVarInsn(ALOAD, index);
				if (isUpvalue) {
					main.visitVarInsn(ALOAD, stackS);
					constantOpcode(main, slot);

					// Redirect the upvalue
					METHOD_UPVALUE_REDIRECT.inject(main);

					// If it is open then store it in the openups too
					dup();
					main.visitVarInsn(ALOAD, openupsS);
					constantOpcode(main, slot);
					main.visitInsn(AASTORE);
				}
			} else {
				loadNil();
			}
			main.visitInsn(AASTORE);
		}

		main.visitVarInsn(ALOAD, 1);
		main.visitVarInsn(ALOAD, stackS);

		// Load variable arguments
		if (((p.is_vararg & Lua.VARARG_NEEDSARG) != 0)) {
			main.visitVarInsn(ALOAD, VARARGS_SLOT);
		} else {
			loadNone();
		}

		constantOpcode(main, pc);

		// Load top
		if (top >= 0 && varargsLocal >= 0) {
			main.visitVarInsn(ALOAD, varargsLocal);
			constantOpcode(main, top);
		} else {
			loadNone();
			main.visitInsn(ICONST_M1);
		}

		main.visitVarInsn(ALOAD, callStackSlot);

		if (upvalues) {
			main.visitVarInsn(ALOAD, openupsS);
		} else {
			main.visitInsn(ACONST_NULL);
		}

		METHOD_RESUME.inject(main);
		main.visitInsn(ARETURN);
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
		constantOpcode(main, index);
		loadVarResult();
		METHOD_RAWSET_LIST.inject(main);
	}

	public void visitConcatValue() {
		METHOD_STRING_CONCAT.inject(main);
	}

	public void visitConcatBuffer() {
		METHOD_BUFFER_CONCAT.inject(main);
	}

	public void visitTobuffer() {
		METHOD_VALUE_TO_BUFFER.inject(main);
	}

	public void visitTovalue() {
		METHOD_BUFFER_TO_VALUE.inject(main);
	}

	public void loadVarargResults(int pc, int a, int vresultbase) {
		if (vresultbase < a) {
			loadVarResult();
			subArgs(a + 1 - vresultbase);
		} else if (vresultbase == a) {
			loadVarResult();
		} else {
			newVarargsVarResult(pc, a, vresultbase - a);
		}
	}

	public void loadLocalOrConstant(int pc, int borc) {
		if (borc <= 0xff) {
			loadLocal(pc, borc);
		} else {
			loadConstant(p.k[borc & 0xff]);
		}
	}
}
