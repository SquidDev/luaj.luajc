/**
 * ****************************************************************************
 * Copyright (c) 2009-2013 Luaj.org. All rights reserved.
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
package org.squiddev.luaj.patches;

import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.ResourceFinder;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StringLibTest implements ResourceFinder {
	public static final String DIRECTORY = "";

	protected LuaTable globals;

	@Before
	public void setup() {
		globals = JsePlatform.debugGlobals();
		BaseLib.FINDER = this;
	}

	@Override
	public InputStream findResource(String filename) {
		return getClass().getResourceAsStream(DIRECTORY + filename);
	}

	@Test
	public void testStringLib() throws Exception {
		runTest("stringlib");
	}

	@Test
	public void testStrings() throws Exception {
		loadScript("strings").call();
	}

	@Test
	public void testStringMisc() throws Exception {
		loadScript("stringMisc").call();
	}

	//region Loading
	/**
	 * Runs a test and compares the output
	 *
	 * @param testName The name of the test file to run
	 */
	public void runTest(String testName) throws Exception {
		// Override print()
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		final PrintStream oldps = BaseLib.instance.STDOUT;
		final PrintStream ps = new PrintStream(output);
		BaseLib.instance.STDOUT = ps;

		// Run the script
		try {
			loadScript(testName).call();

			ps.flush();
			String actualOutput = new String(output.toByteArray());
			String expectedOutput = getExpectedOutput(testName);
			actualOutput = actualOutput.replaceAll("\r\n", "\n");
			expectedOutput = expectedOutput.replaceAll("\r\n", "\n");

			assertEquals(expectedOutput.replace("-nan", "<nan>"), actualOutput);
		} finally {
			BaseLib.instance.STDOUT = oldps;
			ps.close();
		}
	}

	/**
	 * Loads a script into the global table
	 *
	 * @param name The name of the file
	 * @return The loaded LuaFunction
	 * @throws IOException
	 */
	private LuaValue loadScript(String name) throws IOException {
		InputStream script = this.findResource(name + ".lua");
		if (script == null) fail("Could not load script for test case: " + name);
		try {
			return LoadState.load(script, "@" + name + ".lua", globals);
		} finally {
			script.close();
		}
	}

	private String getExpectedOutput(final String name) throws IOException, InterruptedException {
		InputStream output = findResource(name + ".out");
		if (output == null) fail("Failed to get comparison output for " + name);

		try {
			return readString(output);
		} finally {
			output.close();
		}
	}

	private String readString(InputStream is) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int r;
		while ((r = is.read(buf)) >= 0) {
			outputStream.write(buf, 0, r);
		}
		return new String(outputStream.toByteArray());
	}
	//endregion

}
