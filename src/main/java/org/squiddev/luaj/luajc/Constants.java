package org.squiddev.luaj.luajc;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.DebugLib;
import org.objectweb.asm.Type;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.function.FunctionWrapper;
import org.squiddev.luaj.luajc.function.executors.*;
import org.squiddev.luaj.luajc.upvalue.AbstractUpvalue;
import org.squiddev.luaj.luajc.upvalue.UpvalueFactory;
import org.squiddev.luaj.luajc.utils.TinyMethod;

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
	public static final String TYPE_LUAVALUE = Type.getDescriptor(LuaValue.class);
	public static final String CLASS_LUAVALUE = Type.getInternalName(LuaValue.class);
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
			} catch (Exception ignored) {
			}
			return "()V";
		}
	}

	// Manage super classes
	public static final FunctionType[] SUPER_TYPES = new FunctionType[]{
		new FunctionType(ArgExecutor0.class, FunctionWrapper.class),
		new FunctionType(ArgExecutor1.class, FunctionWrapper.class, LuaValue.class),
		new FunctionType(ArgExecutor2.class, FunctionWrapper.class, LuaValue.class, LuaValue.class),
		new FunctionType(ArgExecutor3.class, FunctionWrapper.class, LuaValue.class, LuaValue.class, LuaValue.class),
		new FunctionType(ArgExecutorMany.class, FunctionWrapper.class, Varargs.class),
	};

	// Table functions
	public static final TinyMethod METHOD_TABLEOF = new TinyMethod(LuaValue.class, "tableOf", Varargs.class, int.class);
	public static final TinyMethod METHOD_TABLEOF_DIMS = new TinyMethod(LuaValue.class, "tableOf", int.class, int.class);
	public static final TinyMethod METHOD_TABLE_GET = new TinyMethod(LuaValue.class, "get", LuaValue.class);
	public static final TinyMethod METHOD_TABLE_SET = new TinyMethod(LuaValue.class, "set", LuaValue.class, LuaValue.class);

	// Strings
	public static final TinyMethod METHOD_STRING_CONCAT = new TinyMethod(LuaValue.class, "concat", LuaValue.class);
	public static final TinyMethod METHOD_BUFFER_CONCAT = new TinyMethod(LuaValue.class, "concat", Buffer.class);

	// Varargs
	public static final TinyMethod METHOD_VARARGS_ARG1 = new TinyMethod(Varargs.class, "arg1");
	public static final TinyMethod METHOD_VARARGS_ARG = new TinyMethod(Varargs.class, "arg", int.class);
	public static final TinyMethod METHOD_VARARGS_SUBARGS = new TinyMethod(Varargs.class, "subargs", int.class);

	// Varargs factory
	public static final TinyMethod METHOD_VARARGS_ONE = new TinyMethod(LuaValue.class, "varargsOf", LuaValue.class, Varargs.class);
	public static final TinyMethod METHOD_VARARGS_TWO = new TinyMethod(LuaValue.class, "varargsOf", LuaValue.class, LuaValue.class, Varargs.class);
	public static final TinyMethod METHOD_VARARGS_MANY = new TinyMethod(LuaValue.class, "varargsOf", LuaValue[].class);
	public static final TinyMethod METHOD_VARARGS_MANY_VAR = new TinyMethod(LuaValue.class, "varargsOf", LuaValue[].class, Varargs.class);

	// Type conversion
	public static final TinyMethod METHOD_VALUE_TO_BOOL = new TinyMethod(LuaValue.class, "toboolean");
	public static final TinyMethod METHOD_VALUE_TO_BUFFER = new TinyMethod(LuaValue.class, "buffer");
	public static final TinyMethod METHOD_BUFFER_TO_VALUE = new TinyMethod(Buffer.class, "value");

	// Booleans
	public static final TinyMethod METHOD_TESTFOR_B = new TinyMethod(LuaValue.class, "testfor_b", LuaValue.class, LuaValue.class);
	public static final TinyMethod METHOD_IS_NIL = new TinyMethod(LuaValue.class, "isnil");

	// Calling
	// Normal
	public static final TinyMethod METHOD_CALL_NONE = new TinyMethod(LuaValue.class, "call");
	public static final TinyMethod METHOD_CALL_ONE = new TinyMethod(LuaValue.class, "call", LuaValue.class);
	public static final TinyMethod METHOD_CALL_TWO = new TinyMethod(LuaValue.class, "call", LuaValue.class, LuaValue.class);
	public static final TinyMethod METHOD_CALL_THREE = new TinyMethod(LuaValue.class, "call", LuaValue.class, LuaValue.class, LuaValue.class);

	// Tail call
	public static final TinyMethod METHOD_TAILCALL = new TinyMethod(LuaValue.class, "tailcallOf", LuaValue.class, Varargs.class);

	// Invoke (because that is different to call?) Well, it is but really silly
	public static final TinyMethod METHOD_INVOKE_VAR = new TinyMethod(LuaValue.class, "invoke", Varargs.class);
	public static final TinyMethod METHOD_INVOKE_NONE = new TinyMethod(LuaValue.class, "invoke");
	public static final TinyMethod METHOD_INVOKE_TWO = new TinyMethod(LuaValue.class, "invoke", LuaValue.class, Varargs.class);
	public static final TinyMethod METHOD_INVOKE_THREE = new TinyMethod(LuaValue.class, "invoke", LuaValue.class, LuaValue.class, Varargs.class);

	// ValueOf
	public static final TinyMethod METHOD_VALUEOF_INT = new TinyMethod(LuaValue.class, "valueOf", int.class);
	public static final TinyMethod METHOD_VALUEOF_DOUBLE = new TinyMethod(LuaValue.class, "valueOf", double.class);
	public static final TinyMethod METHOD_VALUEOF_STRING = new TinyMethod(LuaString.class, "valueOf", String.class);
	public static final TinyMethod METHOD_VALUEOF_CHARARRAY = new TinyMethod(LuaString.class, "valueOf", char[].class);

	// Misc
	public static final TinyMethod METHOD_GETENV = new TinyMethod(FunctionWrapper.class, "getfenv");
	public static final TinyMethod METHOD_TO_CHARARRAY = new TinyMethod(String.class, "toCharArray");
	public static final TinyMethod METHOD_RAWSET = new TinyMethod(LuaValue.class, "rawset", int.class, LuaValue.class);
	public static final TinyMethod METHOD_RAWSET_LIST = new TinyMethod(LuaValue.class, "rawsetlist", int.class, Varargs.class);

	// Upvalue creation
	public static final TinyMethod METHOD_NEW_UPVALUE_EMPTY = new TinyMethod(UpvalueFactory.class, "emptyUpvalue");
	public static final TinyMethod METHOD_NEW_UPVALUE_NIL = new TinyMethod(UpvalueFactory.class, "nilUpvalue");
	public static final TinyMethod METHOD_NEW_UPVALUE_VALUE = new TinyMethod(UpvalueFactory.class, "valueUpvalue", LuaValue.class);
	public static final TinyMethod METHOD_NEW_UPVALUE_PROXY = new TinyMethod(UpvalueFactory.class, "proxy", AbstractUpvalue.class);

	// Upvalue modification
	public static final TinyMethod METHOD_SET_UPVALUE = new TinyMethod(AbstractUpvalue.class, "setUpvalue", LuaValue.class);
	public static final TinyMethod METHOD_GET_UPVALUE = new TinyMethod(AbstractUpvalue.class, "getUpvalue");

	// Stack tracing
	public static final TinyMethod METHOD_ONCALL = new TinyMethod(LuaThread.class, "onCall", LuaFunction.class);
	public static final TinyMethod METHOD_ONRETURN = new TinyMethod(LuaThread.CallStack.class, "onReturn");
	public static final TinyMethod METHOD_BYTECODE = new TinyMethod(DebugLib.class, "debugBytecode", DebugLib.DebugState.class, DebugLib.DebugInfo.class, int.class, Varargs.class, int.class);
	public static final TinyMethod METHOD_GETSTATE = new TinyMethod(DebugLib.class, "getDebugState");
	public static final TinyMethod METHOD_GETINFO = new TinyMethod(DebugLib.DebugState.class, "getDebugInfo");

	// Variable naming
	public static final String PREFIX_CONSTANT = "constant";
	public static final String PREFIX_UPVALUE_SLOT = "localUpvalue";
	public static final String PREFIX_LOCAL_SLOT = "local";

	// Super type class
	public static final int SUPERTYPE_VARARGS_ID = 4;
	public static final FunctionType SUPERTYPE_VARARGS = SUPER_TYPES[SUPERTYPE_VARARGS_ID];
	public static final int VARARGS_SLOT = 2;
}
