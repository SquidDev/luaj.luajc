package org.squiddev.luaj.luajc;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;

import java.util.Arrays;
import java.util.Collection;

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
			{"lua/BranchUpvalue"},
			{"lua/BranchUpvalue2"},
			{"lua/DebugInfo"},
			{"lua/DoBlock"},
			{"lua/EdgeCases"},
			{"lua/Error"},
			{"lua/Function"},
			{"lua/LoadBytecode"},
			{"lua/MakeError"},
			{"lua/NForLoop"},
			{"lua/NilCallReturn"},
			{"lua/Recursive"},
			{"lua/RecursiveTrace"},
			{"lua/SetFEnv"},
			{"lua/StringDump"},
			{"lua/TailCall"},
			{"lua/TailRecursion"},
			{"lua/UpvalueArgument"},
			{"lua/Upvalues"},
			{"lua/WhileLoop"},

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
		globals = LuaEnv.makeGlobals();
	}


	/**
	 * Test the {@link LuaJC} compiler.
	 */
	@Test
	public void testLuaJC() throws Exception {
		Loader.install(1);
		run();
	}

	/**
	 * Test the {@link org.squiddev.luaj.luajc.function.LuaVM} implementation.
	 */
	@Test
	public void testLuaVM() throws Exception {
		Loader.install(Integer.MAX_VALUE);
		run();
	}

	/**
	 * A test to compare against the expected behaviour
	 */
	@Test
	public void testLuaC() throws Exception {
		LuaC.install();
		run();
	}

	/**
	 * Check bugs that are in this implementation but not in the original, or vice versa.
	 */
	@Ignore("Some of these fail")
	public void testLuaJCOriginal() throws Exception {
		org.luaj.vm2.luajc.LuaJC.install();
		run();
	}

	protected void run() throws Exception {
		LoadState.load(Loader.load(name), name + ".lua", globals).invoke();
	}
}
