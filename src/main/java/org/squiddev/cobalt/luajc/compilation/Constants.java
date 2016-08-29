package org.squiddev.cobalt.luajc.compilation;

import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.debug.DebugHandler;
import org.squiddev.cobalt.debug.DebugState;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.luajc.analysis.ProtoInfo;
import org.squiddev.cobalt.luajc.function.FunctionWrapper;
import org.squiddev.cobalt.luajc.function.executors.*;
import org.squiddev.cobalt.luajc.upvalue.AbstractUpvalue;
import org.squiddev.cobalt.luajc.utils.CompilationHelper;
import org.squiddev.cobalt.luajc.utils.TinyMethod;
import org.squiddev.cobalt.luajc.utils.TypeFactory;

/**
 * Various constants for the builder
 */
public final class Constants {
	private Constants() {
	}

	public static final String PROTOTYPE_NAME = "prototype";
	public static final String PROTOTYPE_STORAGE = "$Prototypes";
	public static final String EXECUTE_NAME = "execute";

	public static final String TYPE_UPVALUE = Type.getDescriptor(AbstractUpvalue.class);
	public static final String CLASS_STATE = Type.getInternalName(LuaState.class);
	public static final String TYPE_STATE = Type.getDescriptor(LuaState.class);
	public static final String TYPE_HANDLER = Type.getDescriptor(DebugHandler.class);
	public static final String TYPE_LUABOOLEAN = Type.getDescriptor(LuaBoolean.class);
	public static final String CLASS_CONSTANTS = Type.getInternalName(org.squiddev.cobalt.Constants.class);
	public static final String TYPE_LUAVALUE = Type.getDescriptor(LuaValue.class);
	public static final String TYPE_LUATABLE = Type.getDescriptor(LuaTable.class);
	public static final String CLASS_LUAVALUE = Type.getInternalName(LuaValue.class);
	public static final String CLASS_OPERATION = Type.getInternalName(OperationHelper.class);
	public static final String TYPE_PROTOINFO = Type.getDescriptor(ProtoInfo.class);
	public static final String CLASS_PROTOINFO = Type.getInternalName(ProtoInfo.class);
	public static final String CLASS_WRAPPER = Type.getInternalName(FunctionWrapper.class);

	public static final class FunctionType {
		public final String signature;
		public final String className;
		public final int argsLength;

		public FunctionType(String name, String invokeSignature, int args) {
			className = name;
			signature = invokeSignature;
			argsLength = args;
		}

		public FunctionType(Class<?> classObj, Class<?>... args) {
			this(
				Type.getInternalName(classObj),
				getSignature(classObj, EXECUTE_NAME, args),
				args.length
			);
		}

		public static String getSignature(Class<?> classObj, String invokeName, Class... args) {
			try {
				return Type.getMethodDescriptor(classObj.getMethod(invokeName, args));
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	// Manage super classes
	public static final FunctionType[] SUPER_TYPES = new FunctionType[]{
		new FunctionType(ArgExecutor0.class, LuaState.class, FunctionWrapper.class),
		new FunctionType(ArgExecutor1.class, LuaState.class, FunctionWrapper.class, LuaValue.class),
		new FunctionType(ArgExecutor2.class, LuaState.class, FunctionWrapper.class, LuaValue.class, LuaValue.class),
		new FunctionType(ArgExecutor3.class, LuaState.class, FunctionWrapper.class, LuaValue.class, LuaValue.class, LuaValue.class),
		new FunctionType(ArgExecutorMany.class, LuaState.class, FunctionWrapper.class, Varargs.class),
	};

	// Table functions
	public static final TinyMethod METHOD_TABLEOF = new TinyMethod(ValueFactory.class, "tableOf", Varargs.class, int.class);
	public static final TinyMethod METHOD_TABLEOF_DIMS = new TinyMethod(ValueFactory.class, "tableOf", int.class, int.class);
	public static final TinyMethod METHOD_TABLE_GET = new TinyMethod(OperationHelper.class, "getTable", LuaState.class, LuaValue.class, LuaValue.class);
	public static final TinyMethod METHOD_TABLE_SET = new TinyMethod(OperationHelper.class, "setTable", LuaState.class, LuaValue.class, LuaValue.class, LuaValue.class);

	// Strings
	public static final TinyMethod METHOD_STRING_CONCAT = new TinyMethod(OperationHelper.class, "concat", LuaState.class, LuaValue.class, LuaValue.class);
	// public static final TinyMethod METHOD_BUFFER_CONCAT = new TinyMethod(LuaValue.class, "concat", Buffer.class);

	// Varargs
	public static final TinyMethod METHOD_VARARGS_ARG1 = new TinyMethod(Varargs.class, "first");
	public static final TinyMethod METHOD_VARARGS_ARG = new TinyMethod(Varargs.class, "arg", int.class);
	public static final TinyMethod METHOD_VARARGS_SUBARGS = new TinyMethod(Varargs.class, "subargs", int.class);

	// Varargs factory
	public static final TinyMethod METHOD_VARARGS_ONE = new TinyMethod(ValueFactory.class, "varargsOf", LuaValue.class, Varargs.class);
	public static final TinyMethod METHOD_VARARGS_TWO = new TinyMethod(ValueFactory.class, "varargsOf", LuaValue.class, LuaValue.class, Varargs.class);
	public static final TinyMethod METHOD_VARARGS_MANY = new TinyMethod(ValueFactory.class, "varargsOf", LuaValue[].class);
	public static final TinyMethod METHOD_VARARGS_MANY_VAR = new TinyMethod(ValueFactory.class, "varargsOf", LuaValue[].class, Varargs.class);

	// Type conversion
	public static final TinyMethod METHOD_VALUE_TO_BOOL = new TinyMethod(LuaValue.class, "toBoolean");
	// public static final TinyMethod METHOD_VALUE_TO_BUFFER = new TinyMethod(LuaValue.class, "buffer");
	// public static final TinyMethod METHOD_BUFFER_TO_VALUE = new TinyMethod(Buffer.class, "value");

	// Booleans
	public static final TinyMethod METHOD_IS_NIL = new TinyMethod(LuaValue.class, "isNil");

	// Calling
	// Normal
	public static final TinyMethod METHOD_CALL_NONE = new TinyMethod(OperationHelper.class, "call", LuaState.class, LuaValue.class);
	public static final TinyMethod METHOD_CALL_ONE = new TinyMethod(OperationHelper.class, "call", LuaState.class, LuaValue.class, LuaValue.class);
	public static final TinyMethod METHOD_CALL_TWO = new TinyMethod(OperationHelper.class, "call", LuaState.class, LuaValue.class, LuaValue.class, LuaValue.class);
	public static final TinyMethod METHOD_CALL_THREE = new TinyMethod(OperationHelper.class, "call", LuaState.class, LuaValue.class, LuaValue.class, LuaValue.class, LuaValue.class);

	// Tail call
	public static final TinyMethod METHOD_TAILCALL = new TinyMethod(ValueFactory.class, "tailcallOf", LuaValue.class, Varargs.class);

	public static final TinyMethod METHOD_INVOKE_VAR = new TinyMethod(OperationHelper.class, "invoke", LuaState.class, LuaValue.class, Varargs.class);

	// ValueOf
	public static final TinyMethod METHOD_VALUEOF_INT = new TinyMethod(ValueFactory.class, "valueOf", int.class);
	public static final TinyMethod METHOD_VALUEOF_DOUBLE = new TinyMethod(ValueFactory.class, "valueOf", double.class);
	public static final TinyMethod METHOD_VALUEOF_BYTEARRAY = new TinyMethod(ValueFactory.class, "valueOf", byte[].class);

	// Misc
	public static final TinyMethod METHOD_GETENV = new TinyMethod(FunctionWrapper.class, "getfenv");
	public static final TinyMethod METHOD_RAWSET = new TinyMethod(LuaTable.class, "rawset", int.class, LuaValue.class);
	public static final TinyMethod METHOD_RAWSET_LIST = new TinyMethod(CompilationHelper.class, "rawsetList", LuaTable.class, Varargs.class, int.class);
	public static final TinyMethod METHOD_WRAP_ERROR = new TinyMethod(TypeFactory.class, "wrapException", Exception.class);
	public static final TinyMethod METHOD_FILL_TRACEBACK = new TinyMethod(LuaError.class, "fillTraceback", LuaState.class);

	// Upvalue creation
	public static final TinyMethod METHOD_NEW_UPVALUE_EMPTY = new TinyMethod(TypeFactory.class, "emptyUpvalue");
	public static final TinyMethod METHOD_NEW_UPVALUE_NIL = new TinyMethod(TypeFactory.class, "nilUpvalue");
	public static final TinyMethod METHOD_NEW_UPVALUE_VALUE = new TinyMethod(TypeFactory.class, "valueUpvalue", LuaValue.class);
	public static final TinyMethod METHOD_NEW_UPVALUE_PROXY = new TinyMethod(TypeFactory.class, "proxy", AbstractUpvalue.class);

	// Upvalue modification
	public static final TinyMethod METHOD_SET_UPVALUE = new TinyMethod(AbstractUpvalue.class, "setUpvalue", LuaValue.class);
	public static final TinyMethod METHOD_GET_UPVALUE = new TinyMethod(AbstractUpvalue.class, "getUpvalue");

	// Stack tracing
	public static final TinyMethod METHOD_ONCALL = new TinyMethod(DebugHandler.class, "onCall", DebugState.class, LuaClosure.class, Varargs.class, LuaValue[].class);
	public static final TinyMethod METHOD_ONRETURN = new TinyMethod(DebugHandler.class, "onReturn", DebugState.class);
	public static final TinyMethod METHOD_BYTECODE = new TinyMethod(DebugHandler.class, "onInstruction", DebugState.class, DebugFrame.class, int.class, Varargs.class, int.class);
	public static final TinyMethod METHOD_GETSTATE = new TinyMethod(DebugHandler.class, "getDebugState");
	public static final TinyMethod METHOD_GETINFO = new TinyMethod(DebugState.class, "getStack");

	// Variable naming
	public static final String PREFIX_CONSTANT = "constant";
	public static final String PREFIX_UPVALUE_SLOT = "localUpvalue";
	public static final String PREFIX_LOCAL_SLOT = "local";

	// Super type class
	public static final int SUPERTYPE_VARARGS_ID = 4;
	public static final FunctionType SUPERTYPE_VARARGS = SUPER_TYPES[SUPERTYPE_VARARGS_ID];
}
