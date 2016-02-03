package org.squiddev.luaj.luajc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.squiddev.luaj.luajc.analysis.ProtoInfo;
import org.squiddev.luaj.luajc.analysis.type.ConversionAnnotator;
import org.squiddev.luaj.luajc.analysis.type.TypeAnnotator;
import org.squiddev.luaj.luajc.compilation.JavaLoader;
import org.squiddev.luaj.luajc.function.FunctionWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CompilerTest {
	/**
	 * Fetch a list of tests to run
	 *
	 * @return Array of parameters to run. Each array is composed of one element with the name of the test
	 */
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> getLua() {
		return Arrays.asList(new Object[][]{
			{"new-fragment/BranchUpvalue"},
			{"new-fragment/BranchUpvalue2"},
			{"new-fragment/DebugInfo"},
			{"new-fragment/DoBlock"},
			{"new-fragment/EdgeCases"},
			{"new-fragment/Error"},
			{"new-fragment/Function"},
			{"new-fragment/LoadBytecode"},
			{"new-fragment/NForLoop"},
			{"new-fragment/Recursive"},
			{"new-fragment/RecursiveTrace"},
			{"new-fragment/SetFEnv"},
			{"new-fragment/StringDump"},
			{"new-fragment/TailCall"},
			{"new-fragment/TailRecursion"},
			{"new-fragment/UpvalueArgument"},
			{"new-fragment/Upvalues"},
			{"new-fragment/WhileLoop"},

			{"fragment/ForLoopParamUpvalues"},
			{"fragment/VarVarargsUseArg"},
			{"fragment/VarVarargsUseBoth"},
			{"fragment/ArgVarargsUseBoth"},
			{"fragment/ArgParamUseNone"},
			{"fragment/SetlistVarargs"},
			{"fragment/SelfOp"},
			{"fragment/SetListWithOffsetAndVarargs"},
			{"fragment/MultiAssign"},
			{"fragment/Upvalues"},
			{"fragment/NeedsArgAndHasArg"},
			{"fragment/NonAsciiStringLiterals"},
			{"fragment/ControlCharStringLiterals"},
			{"fragment/LoopVarNames"},
			{"fragment/ForLoops"},
			{"fragment/LocalFunctionDeclarations"},
			{"fragment/NilsInTableConstructor"},
			{"fragment/UnreachableCode"},
			{"fragment/VarargsWithParameters"},
			{"fragment/NoReturnValuesPlainCall"},
			{"fragment/VarargsInTableConstructor"},
			{"fragment/VarargsInFirstArg"},
			{"fragment/SetUpvalueTableInitializer"},
			{"fragment/LoadNilUpvalue"},
			{"fragment/UpvalueClosure"},
			{"fragment/UninitializedUpvalue"},
			{"fragment/TestOpUpvalues"},
			{"fragment/TestSimpleBinops"},
			{"fragment/NumericForUpvalues"},
			{"fragment/NumericForUpvalues2"},
			{"fragment/ReturnUpvalue"},
			{"fragment/UninitializedAroundBranch"},
			{"fragment/LoadedNilUpvalue"},
			{"fragment/UpvalueInFirstSlot"},
			{"fragment/ReadOnlyAndReadWriteUpvalues"},
			{"fragment/NestedUpvalues"},
			{"fragment/LoadBool"},
			{"fragment/BasicForLoop"},
			{"fragment/GenericForMultipleValues"},
			{"fragment/AssignReferUpvalues"},
			{"fragment/SimpleRepeatUntil"},
			{"fragment/LoopVarUpvalues"},
			{"fragment/PhiVarUpvalue"},
			{"fragment/UpvaluesInElseClauses"},
		});
	}

	protected final String name;
	protected LuaValue globals;

	public CompilerTest(String name) {
		this.name = name;
	}

	@Before
	public void setup() {
		globals = JsePlatform.debugGlobals();

		JseBaseLib lib = new JseBaseLib();
		lib.STDOUT = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
			}
		});

		globals.load(lib);
		globals.set("assertEquals", new AssertFunction());
		globals.set("assertMany", new AssertManyFunction());
	}

	/**
	 * Get the Lua test and run it
	 */
	@Test
	public void testLuaJC() throws Exception {
		LuaJC.install(new CompileOptions(CompileOptions.PREFIX, 1, CompileOptions.TYPE_THRESHOLD, true));
		run();
	}

	@Test
	public void testTypes() throws Exception {
		Prototype proto = Loader.loadPrototype(name, name + ".lua");
		JavaLoader loader = new JavaLoader(new CompileOptions(), LuaJC.toClassName(name), name + ".lua");

		ProtoInfo info = new ProtoInfo(proto, loader);

		FunctionWrapper function = new FunctionWrapper(info, globals);
		for (int i = 0; i < 9; i++) {
			function.call();
		}

		new TypeAnnotator(info).fill(0.7f);
		new ConversionAnnotator(info).fill();
	}

	@Test
	public void testLuaC() throws Exception {
		LuaC.install();
		run();
	}

	protected void run() throws Exception {
		LoadState.load(Loader.load(name), name + ".lua", globals).invoke();
	}

	private class AssertFunction extends ThreeArgFunction {
		@Override
		public LuaValue call(LuaValue expected, LuaValue actual, LuaValue message) {
			String msg = message.toString();
			if (message.isnil()) {
				msg = "(No message)";
			}

			assertEquals(msg, expected.tojstring(), actual.tojstring());
			assertEquals(msg, expected.typename(), actual.typename());

			return LuaValue.NONE;
		}
	}

	private class AssertManyFunction extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs args) {
			int nArgs = args.narg() / 2;
			for (int i = 1; i <= nArgs; i++) {
				LuaValue expected = args.arg(i);
				LuaValue actual = args.arg(i + nArgs);

				assertEquals("Type mismatch at arg #" + i, expected.typename(), actual.typename());
				assertEquals("Value mismatch at arg #" + i, expected.tojstring(), actual.tojstring());
			}


			return LuaValue.NONE;
		}
	}
}
