/**
 * ****************************************************************************
 * Copyright (c) 2010 Luaj.org. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ****************************************************************************
 */
package org.squiddev.luaj.luajc;

import org.luaj.vm2.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.luaj.luajc.function.*;
import org.squiddev.luaj.luajc.utils.TinyMethod;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.luajc.utils.AsmUtils.constantOpcode;

public class JavaBuilder {
	public static final String PROTOTYPE_NAME = "PROTOTYPE";

	protected static final String TYPE_LOCALUPVALUE = Type.getDescriptor(LuaValue[].class);
	protected static final String TYPE_LUAVALUE = Type.getDescriptor(LuaValue.class);
	protected static final String CLASS_LUAVALUE = Type.getInternalName(LuaValue.class);

	protected static final String TYPE_CALLSTACK = Type.getDescriptor(LuaThread.CallStack.class);
	protected static final String TYPE_PROTOTYPE = Type.getDescriptor(Prototype.class);
	protected static final String TYPE_SOURCE = Type.getDescriptor(LuaCompiledSource.class);
	protected static final String TYPE_COMPILED = Type.getDescriptor(LuaCompiledFunction.class);
	protected static final String CLASS_SOURCE = Type.getInternalName(LuaCompiledSource.class);
	protected static final String CLASS_COMPILED = Type.getInternalName(LuaCompiledFunction.class);

	protected static class FunctionType {
		public final String signature;
		public final String methodName;
		public final String className;
		public final int argsLength;

		public FunctionType(String name, String invokeName, String invokeSignature, int args) {
			className = name;
			methodName = invokeName;
			signature = invokeSignature;
			argsLength = args;
		}

		public FunctionType(Class<?> classObj, String invokeName, Class<?>... args) {
			this(
				Type.getInternalName(classObj),
				invokeName, getSignature(classObj, invokeName, args),
				args.length
			);
		}

		protected static String getSignature(Class<?> classObj, String invokeName, Class... args) {
			try {
				return Type.getMethodDescriptor(classObj.getMethod(invokeName, args));
			} catch (Exception ignored) {
			}
			return "()V";
		}
	}

	// Manage super classes
	protected static FunctionType[] SUPER_TYPES = {
		new FunctionType(ZeroArgFunction.class, "call"),
		new FunctionType(OneArgFunction.class, "call", LuaValue.class),
		new FunctionType(TwoArgFunction.class, "call", LuaValue.class, LuaValue.class),
		new FunctionType(ThreeArgFunction.class, "call", LuaValue.class, LuaValue.class, LuaValue.class),
		new FunctionType(VarArgFunction.class, "invoke", Varargs.class),
	};

	// Table functions
	protected static final TinyMethod METHOD_TABLEOF = new TinyMethod(LuaValue.class, "tableOf", Varargs.class, int.class);
	protected static final TinyMethod METHOD_TABLEOF_DIMS = new TinyMethod(LuaValue.class, "tableOf", int.class, int.class);
	protected static final TinyMethod METHOD_TABLE_GET = new TinyMethod(LuaValue.class, "get", LuaValue.class);
	protected static final TinyMethod METHOD_TABLE_SET = new TinyMethod(LuaValue.class, "set", LuaValue.class, LuaValue.class);

	// Strings
	protected static final TinyMethod METHOD_STRING_CONCAT = new TinyMethod(LuaValue.class, "concat", LuaValue.class);
	protected static final TinyMethod METHOD_BUFFER_CONCAT = new TinyMethod(LuaValue.class, "concat", Buffer.class);

	// Varargs
	protected static final TinyMethod METHOD_VARARGS_ARG1 = new TinyMethod(Varargs.class, "arg1");
	protected static final TinyMethod METHOD_VARARGS_ARG = new TinyMethod(Varargs.class, "arg", int.class);
	protected static final TinyMethod METHOD_VARARGS_SUBARGS = new TinyMethod(Varargs.class, "subargs", int.class);

	// Varargs factory
	protected static final TinyMethod METHOD_VARARGS_ONE = new TinyMethod(LuaValue.class, "varargsOf", LuaValue.class, Varargs.class);
	protected static final TinyMethod METHOD_VARARGS_TWO = new TinyMethod(LuaValue.class, "varargsOf", LuaValue.class, LuaValue.class, Varargs.class);
	protected static final TinyMethod METHOD_VARARGS_MANY = new TinyMethod(LuaValue.class, "varargsOf", LuaValue[].class);
	protected static final TinyMethod METHOD_VARARGS_MANY_VAR = new TinyMethod(LuaValue.class, "varargsOf", LuaValue[].class, Varargs.class);

	// Type conversion
	protected static final TinyMethod METHOD_VALUE_TO_BOOL = new TinyMethod(LuaValue.class, "toboolean");
	protected static final TinyMethod METHOD_BUFFER_TO_STR = new TinyMethod(Buffer.class, "tostring");
	protected static final TinyMethod METHOD_VALUE_TO_BUFFER = new TinyMethod(LuaValue.class, "buffer");
	protected static final TinyMethod METHOD_BUFFER_TO_VALUE = new TinyMethod(Buffer.class, "value");

	// Booleans
	protected static final TinyMethod METHOD_TESTFOR_B = new TinyMethod(LuaValue.class, "testfor_b", LuaValue.class, LuaValue.class);
	protected static final TinyMethod METHOD_IS_NIL = new TinyMethod(LuaValue.class, "isnil");

	// Calling
	// Normal
	protected static final TinyMethod METHOD_CALL_NONE = new TinyMethod(LuaValue.class, "call");
	protected static final TinyMethod METHOD_CALL_ONE = new TinyMethod(LuaValue.class, "call", LuaValue.class);
	protected static final TinyMethod METHOD_CALL_TWO = new TinyMethod(LuaValue.class, "call", LuaValue.class, LuaValue.class);
	protected static final TinyMethod METHOD_CALL_THREE = new TinyMethod(LuaValue.class, "call", LuaValue.class, LuaValue.class, LuaValue.class);

	// Tail call
	protected static final TinyMethod METHOD_TAILCALL = new TinyMethod(LuaValue.class, "tailcallOf", LuaValue.class, Varargs.class);
	protected static final TinyMethod METHOD_TAILCALL_EVAL = new TinyMethod(Varargs.class, "eval");

	// Invoke (because that is different to call?) Well, it is but really silly
	protected static final TinyMethod METHOD_INVOKE_VAR = new TinyMethod(LuaValue.class, "invoke", Varargs.class);
	protected static final TinyMethod METHOD_INVOKE_NONE = new TinyMethod(LuaValue.class, "invoke");
	protected static final TinyMethod METHOD_INVOKE_TWO = new TinyMethod(LuaValue.class, "invoke", LuaValue.class, Varargs.class);
	protected static final TinyMethod METHOD_INVOKE_THREE = new TinyMethod(LuaValue.class, "invoke", LuaValue.class, LuaValue.class, Varargs.class);

	// ValueOf
	protected static final TinyMethod METHOD_VALUEOF_INT = new TinyMethod(LuaValue.class, "valueOf", int.class);
	protected static final TinyMethod METHOD_VALUEOF_DOUBLE = new TinyMethod(LuaValue.class, "valueOf", double.class);
	protected static final TinyMethod METHOD_VALUEOF_STRING = new TinyMethod(LuaString.class, "valueOf", String.class);
	protected static final TinyMethod METHOD_VALUEOF_CHARARRAY = new TinyMethod(LuaString.class, "valueOf", char[].class);

	// Misc
	protected static final TinyMethod METHOD_SETENV = new TinyMethod(LuaValue.class, "setfenv", LuaValue.class);
	protected static final TinyMethod METHOD_TO_CHARARRAY = new TinyMethod(String.class, "toCharArray");
	protected static final TinyMethod METHOD_RAWSET = new TinyMethod(LuaValue.class, "rawset", int.class, LuaValue.class);
	protected static final TinyMethod METHOD_RAWSET_LIST = new TinyMethod(LuaValue.class, "rawsetlist", int.class, Varargs.class);

	// Upvalue creation
	protected static final TinyMethod METHOD_NEW_UPVALUE_EMPTY = new TinyMethod(LuaCompiledFunction.class, "newupe");
	protected static final TinyMethod METHOD_NEW_UPVALUE_NIL = new TinyMethod(LuaCompiledFunction.class, "newupn");
	protected static final TinyMethod METHOD_NEW_UPVALUE_VALUE = new TinyMethod(LuaCompiledFunction.class, "newupl", LuaValue.class);

	// Stack tracing
	protected static final TinyMethod METHOD_ONCALL = new TinyMethod(LuaThread.class, "onCall", LuaFunction.class);
	protected static final TinyMethod METHOD_ONRETURN = new TinyMethod(LuaThread.CallStack.class, "onReturn");

	// Variable naming
	protected static final String PREFIX_CONSTANT = "k";
	protected static final String PREFIX_UPVALUE = "u";
	protected static final String PREFIX_UPVALUE_SLOT = "a";
	protected static final String PREFIX_LOCAL_SLOT = "s";

	// Basic info
	protected final ProtoInfo pi;
	protected final Prototype p;
	protected final String className;

	/**
	 * Main class writer
	 */
	protected final ClassWriter writer;

	/**
	 * The static constructor method
	 */
	protected final MethodVisitor init;

	/**
	 * The function invoke
	 */
	protected final MethodVisitor main;

	/**
	 * Max number of locals
	 */
	protected int maxLocals;

	/**
	 * The local index of the varargs result
	 */
	protected int varargsLocal = -1;

	Label start;
	Label end;

	// the superclass arg count, 0-3 args, 4=varargs
	protected FunctionType superclass;
	protected static int SUPERTYPE_VARARGS_ID = 4;
	protected static FunctionType SUPERTYPE_VARARGS = SUPER_TYPES[SUPERTYPE_VARARGS_ID];

	/**
	 * Go to destinations
	 */
	protected final Label[] branchDestinations;

	/**
	 * Store the previous line number
	 */
	protected int previousLine = -1;

	/**
	 * The slot for the LuaSource
	 */
	protected int sourceSlot = -1;

	/**
	 * The slot for the LuaThread.CallStack
	 */
	protected int callStackSlot = -1;

	/**
	 * The current program counter
	 */
	protected int pc = 0;

	public JavaBuilder(ProtoInfo pi, String className, String filename) {
		this.pi = pi;
		this.p = pi.prototype;

		this.className = className;

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
		/*
			We don't need to compute frames as slots do not change their type
			TODO: Auto compute maxes and locals.
		 */
		writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		// Check the name of the class. We have no interfaces and no generics
		writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className, null, superType.className, null);

		// Write the filename
		writer.visitSource(filename, null);

		// Create the fields
		for (int i = 0; i < p.nups; i++) {
			boolean isReadWrite = pi.isReadWriteUpvalue(pi.upvalues[i]);
			String type = isReadWrite ? TYPE_LOCALUPVALUE : TYPE_LUAVALUE;
			writer.visitField(0, upvalueName(i), type, null, null);
		}

		// Stores the prototype object
		writer.visitField(ACC_PUBLIC + ACC_STATIC, PROTOTYPE_NAME, TYPE_PROTOTYPE, null, null).visitEnd();

		// Create the class constructor
		init = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		init.visitCode();

		// Create the invoke method
		main = writer.visitMethod(ACC_PUBLIC + ACC_FINAL, superType.methodName, superType.signature, null, null);
		main.visitCode();

		{
			// Create some information
			start = new Label();
			end = new Label();
			main.visitLabel(start);

			// Create the slots for current line and stack
			sourceSlot = ++maxLocals;
			callStackSlot = ++maxLocals;

			// Create source object
			main.visitTypeInsn(NEW, CLASS_SOURCE);
			main.visitInsn(DUP);

			// Constructor
			main.visitVarInsn(ALOAD, 0);
			main.visitMethodInsn(INVOKESPECIAL, CLASS_SOURCE, "<init>", "(" + TYPE_COMPILED + ")V", false);

			// Store it in sourceSlot
			main.visitInsn(DUP);
			main.visitVarInsn(ASTORE, sourceSlot);

			// On method call, store callstack in slot
			METHOD_ONCALL.inject(main);
			main.visitVarInsn(ASTORE, callStackSlot);
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

		{
			// Add default constructor
			MethodVisitor construct = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			construct.visitVarInsn(ALOAD, 0);
			construct.visitMethodInsn(INVOKESPECIAL, superclass.className, "<init>", "()V", false);

			construct.visitVarInsn(ALOAD, 0);
			construct.visitLdcInsn(filename);
			construct.visitFieldInsn(PUTFIELD, CLASS_COMPILED, "source", "Ljava/lang/String;");

			construct.visitVarInsn(ALOAD, 0);
			constantOpcode(construct, p.linedefined);
			construct.visitFieldInsn(PUTFIELD, CLASS_COMPILED, "startLine", "I");

			construct.visitInsn(RETURN);
			construct.visitMaxs(2, 1);
			construct.visitEnd();
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
				if (pi.isInitialValueUsed(slot)) {
					main.visitVarInsn(ALOAD, 1);
					constantOpcode(main, slot + 1);
					METHOD_VARARGS_ARG.inject(main, INVOKEVIRTUAL);
					storeLocal(-1, slot);
				}
			}
			boolean needsArg = ((p.is_vararg & Lua.VARARG_NEEDSARG) != 0);
			if (needsArg) {
				main.visitVarInsn(ALOAD, 1);
				constantOpcode(main, p.numparams + 1);
				METHOD_TABLEOF.inject(main, INVOKESTATIC);
				storeLocal(-1, slot++);
			} else if (p.numparams > 0) {
				main.visitVarInsn(ALOAD, 1);
				constantOpcode(main, p.numparams + 1);
				METHOD_VARARGS_SUBARGS.inject(main, INVOKEVIRTUAL);
				main.visitVarInsn(ASTORE, 1);
			}
		} else {
			// fixed arg function between 0 and 3 arguments
			for (slot = 0; slot < p.numparams; slot++) {
				this.plainSlotVars.put(slot, slot + 1);
				if (pi.isUpvalueCreate(-1, slot)) {
					main.visitVarInsn(ALOAD, 1);
					storeLocal(-1, slot);
				}
			}
		}

		// nil parameters
		for (; slot < p.maxstacksize; slot++) {
			if (pi.isInitialValueUsed(slot)) {
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
		for (Map.Entry<Integer, Integer> slot : plainSlotVars.entrySet()) {
			main.visitLocalVariable(PREFIX_LOCAL_SLOT + "_" + slot.getKey(), TYPE_LUAVALUE, null, start, end, slot.getValue());
		}

		for (Map.Entry<Integer, Integer> slot : upvalueSlotVars.entrySet()) {
			main.visitLocalVariable(PREFIX_UPVALUE_SLOT + "_" + slot.getKey(), TYPE_LOCALUPVALUE, null, start, end, slot.getValue());
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

	protected Map<Integer, Integer> plainSlotVars = new HashMap<>();
	protected Map<Integer, Integer> upvalueSlotVars = new HashMap<>();

	protected int findSlot(int luaSlot, Map<Integer, Integer> map) {
		if (map.containsKey(luaSlot)) return map.get(luaSlot);

		// This will always be an Upvalue/LuaValue so the slot size is 1 as it is a reference
		int javaSlot = ++maxLocals;
		map.put(luaSlot, javaSlot);
		return javaSlot;
	}

	protected int findSlotIndex(int slot, boolean isUpvalue) {
		return isUpvalue ?
			findSlot(slot, upvalueSlotVars) :
			findSlot(slot, plainSlotVars);
	}

	public void loadLocal(int pc, int slot) {
		boolean isUpvalue = pi.isUpvalueRefer(pc, slot);
		int index = findSlotIndex(slot, isUpvalue);

		main.visitVarInsn(ALOAD, index);
		if (isUpvalue) {
			main.visitInsn(ICONST_0);
			main.visitInsn(AALOAD);
		}
	}

	public void storeLocal(int pc, int slot) {
		boolean isUpvalue = pi.isUpvalueAssign(pc, slot);
		int index = findSlotIndex(slot, isUpvalue);
		if (isUpvalue) {
			boolean isUpCreate = pi.isUpvalueCreate(pc, slot);
			if (isUpCreate) {
				// If we are creating the upvalue for the first time then we call LibFunction.newupe (but actually call
				// <className>.newupe but I need to check that). The we duplicate the object, so it remains on the stack
				// and store it
				METHOD_NEW_UPVALUE_EMPTY.inject(main);
				main.visitInsn(DUP);
				main.visitVarInsn(ASTORE, index);
			} else {
				main.visitVarInsn(ALOAD, index);
			}

			// We swap the values which is the value and the array
			// Then we get item 0 of the array
			// And store to it
			main.visitInsn(SWAP);
			main.visitIntInsn(ICONST_0, 0);
			main.visitInsn(SWAP);
			main.visitInsn(AASTORE);
		} else {
			main.visitVarInsn(ASTORE, index);
		}
	}

	public void createUpvalues(int pc, int firstSlot, int numSlots) {
		for (int i = 0; i < numSlots; i++) {
			int slot = firstSlot + i;
			boolean isupcreate = pi.isUpvalueCreate(pc, slot);
			if (isupcreate) {
				int index = findSlotIndex(slot, true);
				METHOD_NEW_UPVALUE_NIL.inject(main);
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
			int upvalueIndex = findSlotIndex(slot, true);
			main.visitVarInsn(ASTORE, upvalueIndex);
		}
	}

	protected static String upvalueName(int upvalueIndex) {
		return PREFIX_UPVALUE + upvalueIndex;
	}

	public void loadUpvalue(int upvalueIndex) {
		boolean isReadWrite = pi.isReadWriteUpvalue(pi.upvalues[upvalueIndex]);
		main.visitVarInsn(ALOAD, 0);

		if (isReadWrite) {
			// We get the first value of the array in <classname>.<upvalueName>
			main.visitFieldInsn(GETFIELD, className, upvalueName(upvalueIndex), TYPE_LOCALUPVALUE);
			main.visitInsn(ICONST_0);
			main.visitInsn(AALOAD);
		} else {
			// Not a 'proper' upvalue, so we just need to get the value itself
			main.visitFieldInsn(GETFIELD, className, upvalueName(upvalueIndex), TYPE_LUAVALUE);
		}
	}

	public void storeUpvalue(int pc, int upvalueIndex, int slot) {
		boolean isReadWrite = pi.isReadWriteUpvalue(pi.upvalues[upvalueIndex]);
		main.visitVarInsn(ALOAD, 0);
		if (isReadWrite) {
			// We set the first value of the array in <classname>.<upvalueName>
			main.visitFieldInsn(GETFIELD, className, upvalueName(upvalueIndex), TYPE_LOCALUPVALUE);
			main.visitInsn(ICONST_0);
			loadLocal(pc, slot);
			main.visitInsn(AASTORE);
		} else {
			loadLocal(pc, slot);
			main.visitFieldInsn(PUTFIELD, className, upvalueName(upvalueIndex), TYPE_LUAVALUE);
		}
	}

	public void newTable(int b, int c) {
		constantOpcode(main, b);
		constantOpcode(main, c);
		METHOD_TABLEOF_DIMS.inject(main);
	}

	public void loadEnv() {
		main.visitVarInsn(ALOAD, 0);
		main.visitFieldInsn(GETFIELD, className, "env", TYPE_LUAVALUE);
	}

	public void loadVarargs() {
		main.visitVarInsn(ALOAD, 1);
	}

	public void loadVarargs(int argindex) {
		loadVarargs();
		arg(argindex);
	}

	public void arg(int argindex) {
		if (argindex == 1) {
			METHOD_VARARGS_ARG1.inject(main);
		} else {
			constantOpcode(main, argindex);
			METHOD_VARARGS_ARG.inject(main);
		}
	}

	protected int getVarresultIndex() {
		if (varargsLocal < 0) varargsLocal = ++maxLocals;
		return varargsLocal;
	}

	public void loadVarresult() {
		main.visitVarInsn(ALOAD, getVarresultIndex());
	}

	public void storeVarresult() {
		main.visitVarInsn(ASTORE, getVarresultIndex());
	}

	public void subargs(int firstarg) {
		constantOpcode(main, firstarg);
		METHOD_VARARGS_SUBARGS.inject(main);
	}

	public void getTable() {
		METHOD_TABLE_GET.inject(main);
	}

	public void setTable() {
		METHOD_TABLE_SET.inject(main);
	}

	public void unaryop(int o) {
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

		// TODO: More constants, less magic variables
		main.visitMethodInsn(INVOKEVIRTUAL, CLASS_LUAVALUE, op, "()" + TYPE_LUAVALUE, false);
	}

	public void binaryop(int o) {
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

	public void compareop(int o) {
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

	public void areturn() {
		// Pop call stack
		main.visitVarInsn(ALOAD, callStackSlot);
		METHOD_ONRETURN.inject(main);

		main.visitInsn(ARETURN);
	}

	public void toBoolean() {
		METHOD_VALUE_TO_BOOL.inject(main);
	}

	public void tostring() {
		METHOD_BUFFER_TO_STR.inject(main);
	}

	public void isNil() {
		METHOD_IS_NIL.inject(main);
	}

	public void testForLoop() {
		METHOD_TESTFOR_B.inject(main);
	}

	public void loadArrayArgs(int pc, int firstslot, int nargs) {
		constantOpcode(main, nargs);
		main.visitTypeInsn(ANEWARRAY, CLASS_LUAVALUE);
		for (int i = 0; i < nargs; i++) {
			main.visitInsn(DUP);
			constantOpcode(main, i);
			loadLocal(pc, firstslot++);
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

	public void newVarargsVarresult(int pc, int firstslot, int nslots) {
		loadArrayArgs(pc, firstslot, nslots);
		loadVarresult();
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
		METHOD_TAILCALL_EVAL.inject(main);
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


	// ------------------------ closures ------------------------

	public void closureCreate(String protoname) {
		main.visitTypeInsn(NEW, protoname);
		main.visitInsn(DUP);
		main.visitMethodInsn(INVOKESPECIAL, protoname, "<init>", "()V", false);
		main.visitInsn(DUP);
		loadEnv();
		METHOD_SETENV.inject(main);
	}

	public void closureInitUpvalueFromUpvalue(String protoName, int newUpvalue, int upvalueIndex) {
		boolean isReadWrite = pi.isReadWriteUpvalue(pi.upvalues[upvalueIndex]);

		String type = isReadWrite ? TYPE_LOCALUPVALUE : TYPE_LUAVALUE;
		String srcName = upvalueName(upvalueIndex);
		String destName = upvalueName(newUpvalue);

		main.visitVarInsn(ALOAD, 0);
		// Get from one field and set to the other
		main.visitFieldInsn(GETFIELD, className, srcName, type);
		main.visitFieldInsn(PUTFIELD, protoName, destName, type);
	}

	public void closureInitUpvalueFromLocal(String protoName, int newUpvalue, int pc, int srcSlot) {
		boolean isReadWrite = pi.isReadWriteUpvalue(pi.vars[srcSlot][pc].upvalue);
		String type = isReadWrite ? TYPE_LOCALUPVALUE : TYPE_LUAVALUE;
		String destName = upvalueName(newUpvalue);
		int index = findSlotIndex(srcSlot, isReadWrite);

		main.visitVarInsn(ALOAD, index);
		main.visitFieldInsn(PUTFIELD, protoName, destName, type);
	}

	protected Map<LuaValue, String> constants = new HashMap<>();

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
					name = value.type() == LuaValue.TNUMBER ?
						value.isinttype() ?
							createLuaIntegerField(value.checkint()) :
							createLuaDoubleField(value.checkdouble()) :
						createLuaStringField(value.checkstring());
					constants.put(value, name);
				}
				main.visitFieldInsn(GETSTATIC, className, name, TYPE_LUAVALUE);
				break;
			default:
				throw new IllegalArgumentException("bad constant type: " + value.type());
		}
	}

	protected String createLuaIntegerField(int value) {
		String name = PREFIX_CONSTANT + constants.size();
		writer.visitField(ACC_STATIC | ACC_FINAL, name, TYPE_LUAVALUE, null, null);

		constantOpcode(init, value);
		METHOD_VALUEOF_INT.inject(init);
		init.visitFieldInsn(PUTSTATIC, className, name, TYPE_LUAVALUE);
		return name;
	}

	protected String createLuaDoubleField(double value) {
		String name = PREFIX_CONSTANT + constants.size();
		writer.visitField(ACC_STATIC | ACC_FINAL, name, TYPE_LUAVALUE, null, null);
		constantOpcode(init, value);
		METHOD_VALUEOF_DOUBLE.inject(init);
		init.visitFieldInsn(PUTSTATIC, className, name, TYPE_LUAVALUE);
		return name;
	}

	protected String createLuaStringField(LuaString value) {
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

	// --------------------- branching support -------------------------
	public static final int BRANCH_GOTO = 1;
	public static final int BRANCH_IFNE = 2;
	public static final int BRANCH_IFEQ = 3;

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
		this.pc = pc;
		Label currentLabel = branchDestinations[pc];

		main.visitLabel(currentLabel);

		int[] lineInfo = p.lineinfo;
		int currentLine;
		if (lineInfo != null && lineInfo.length > pc && (currentLine = lineInfo[pc]) != previousLine) {
			main.visitLineNumber(currentLine, currentLabel);
			main.visitVarInsn(ALOAD, sourceSlot);
			constantOpcode(main, currentLine);
			main.visitFieldInsn(PUTFIELD, CLASS_SOURCE, "line", "I");
			previousLine = currentLine;
		}
	}

	public void setlistStack(int pc, int a0, int index0, int nvals) {
		for (int i = 0; i < nvals; i++) {
			main.visitInsn(DUP);
			constantOpcode(main, index0 + i);
			loadLocal(pc, a0 + i);
			METHOD_RAWSET.inject(main);
		}
	}

	public void setlistVarargs(int index) {
		constantOpcode(main, index);
		loadVarresult();
		METHOD_RAWSET_LIST.inject(main);
	}

	public void concatvalue() {
		METHOD_STRING_CONCAT.inject(main);
	}

	public void concatbuffer() {
		METHOD_BUFFER_CONCAT.inject(main);
	}

	public void tobuffer() {
		METHOD_VALUE_TO_BUFFER.inject(main);
	}

	public void tovalue() {
		METHOD_BUFFER_TO_VALUE.inject(main);
	}
}
