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
package org.squiddev.luaj.luajc.analysis;

import org.luaj.vm2.Print;
import org.luaj.vm2.Prototype;
import org.squiddev.luaj.luajc.analysis.block.BasicBlock;
import org.squiddev.luaj.luajc.compilation.JavaLoader;
import org.squiddev.luaj.luajc.function.FunctionExecutor;
import org.squiddev.luaj.luajc.function.executors.ClosureExecutor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * Prototype information for static single-assignment analysis
 */
public final class ProtoInfo {
	//region Execution
	/**
	 * The executor to currently use
	 */
	public FunctionExecutor executor = ClosureExecutor.INSTANCE;

	/**
	 * Number of times this method has been called from a closure.
	 */
	public int calledClosure = 0;

	/**
	 * Threshold before compiling.
	 *
	 * @see org.squiddev.luaj.luajc.CompileOptions#compileThreshold
	 */
	public final int threshold;

	/**
	 * The loader for this prototype
	 */
	public final JavaLoader loader;
	//endregion

	/**
	 * The name of the prototype
	 */
	public final String name;

	/**
	 * The prototype that this info is about
	 */
	public final Prototype prototype;

	//region Analysis code
	/**
	 * List of child prototypes or null
	 */
	public final ProtoInfo[] subprotos;

	/**
	 * List of blocks for analysis of code branching
	 */
	public final BasicBlock[] blocks;

	/**
	 * Blocks in breadth-first order
	 */
	public final BasicBlock[] blockList;

	/**
	 * Parameters and entry values of stack variables
	 */
	public final VarInfo[] params;

	/**
	 * Variables in the form vars[pc][slot].
	 */
	public final VarInfo[][] vars;

	/**
	 * List of upvalues from outer scope
	 */
	public final UpvalueInfo[] upvalues;

	public final List<VarInfo>[] phiPositions;

	public final List<PhiInfo> phis;
	//endregion

	public ProtoInfo(Prototype p, JavaLoader loader) {
		this(p, loader, "", null);
	}

	protected ProtoInfo(Prototype p, JavaLoader loader, String name, UpvalueInfo[] u) {
		this.name = name;
		this.loader = loader;
		prototype = p;
		threshold = loader.options.compileThreshold;
		upvalues = u;
		subprotos = p.p != null && p.p.length > 0 ? new ProtoInfo[p.p.length] : null;

		// find basic blocks
		blocks = BasicBlock.findBasicBlocks(p);
		blockList = BasicBlock.findLiveBlocks(blocks);

		// params are inputs to first block
		params = new VarInfo[p.maxstacksize];
		vars = new VarInfo[prototype.code.length][];

		//noinspection unchecked
		phiPositions = new List[prototype.code.length];

		AnalysisBuilder builder = new AnalysisBuilder(this);
		builder.fillArguments();
		phis = builder.findVariables();
		builder.findUpvalues();

		if (loader.options.compileThreshold <= 0) {
			executor = loader.include(this);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		// prototype name
		sb.append("proto '").append(name).append("'\n");

		// upvalues from outer scopes
		for (int i = 0, n = (upvalues != null ? upvalues.length : 0); i < n; i++) {
			sb.append("\tup[").append(i).append("]: ").append(upvalues[i]).append("\n");
		}

		// basic blocks
		for (BasicBlock b : blockList) {
			int pc0 = b.pc0;
			sb.append("\tblock ").append(b.toString()).append('\n');
			appendOpenUps(sb, -1);

			// instructions
			for (int pc = pc0; pc <= b.pc1; pc++) {

				// open upvalue storage
				appendOpenUps(sb, pc);

				// opcode
				sb.append("\t\t");
				VarInfo[] vars = this.vars[pc];
				for (VarInfo v : vars) {
					String u;
					if (v == null) {
						u = "";
					} else if (v.upvalue == null) {
						u = "    ";
					} else if (!v.upvalue.readWrite) {
						u = "[C] ";
					} else {
						u = (v.allocUpvalue && v.pc == pc ? "[*] " : "[]  ");
					}
					String s = v == null ? "null   " : String.valueOf(v);
					sb.append(s).append(u);
				}
				sb.append("  ");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream ops = Print.ps;
				Print.ps = new PrintStream(baos);
				try {
					Print.printOpCode(prototype, pc);
				} finally {
					Print.ps.close();
					Print.ps = ops;
				}
				sb.append(baos.toString());
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	private void appendOpenUps(StringBuilder sb, int pc) {
		VarInfo[] vars = (pc < 0 ? params : this.vars[pc]);
		for (int i = 0; i < prototype.maxstacksize; i++) {
			VarInfo v = vars[i];
			if (v != null && v.pc == pc && v.allocUpvalue) {
				sb.append("\t\topen: ").append(v.upvalue).append("\n");
			}
		}
	}

	/**
	 * Check if this is an assignment to an upvalue
	 *
	 * @param pc   The current PC
	 * @param slot The slot the upvalue is stored in
	 * @return If an upvalue is assigned to at this point
	 */
	public boolean isUpvalueAssign(int pc, int slot) {
		VarInfo v = pc < 0 ? params[slot] : vars[pc][slot];
		return v != null && v.upvalue != null && v.upvalue.readWrite;
	}

	/**
	 * Check if this is the creation of an upvalue
	 *
	 * @param pc   The current PC
	 * @param slot The slot the upvalue is stored in
	 * @return If this is where the upvalue is created
	 */
	public boolean isUpvalueCreate(int pc, int slot) {
		VarInfo v = pc < 0 ? params[slot] : vars[pc][slot];
		return v != null && v.upvalue != null && v.upvalue.readWrite && v.allocUpvalue && pc == v.pc;
	}

	/**
	 * Check if this is the creation of an upvalue
	 *
	 * @param pc The current PC
	 * @return If this is where the upvalue is created
	 */
	public boolean isUpvalueCreate(VarInfo v, int pc) {
		return v != null && v.upvalue != null && v.upvalue.readWrite && v.allocUpvalue && pc == v.pc;
	}

	/**
	 * Get the definition of this variable
	 *
	 * @param pc   The current PC
	 * @param slot The slot the upvalue is stored in
	 * @return If this is a reference to a read/write upvalue
	 */
	public VarInfo getVariable(int pc, int slot) {
		if (pc < 0) {
			return params[slot];
		}

		VarInfo info = vars[pc][slot];

		if (info.pc == pc || info == VarInfo.INVALID) {
			// special case when both refer and assign in same instruction
			// This probably isn't accurate
			BasicBlock currentBlock = blocks[pc];

			VarInfo previous;
			if (pc == currentBlock.pc0) {
				previous = currentBlock.entry[slot];
			} else {
				previous = vars[pc - 1][slot];
			}

			return previous;
		}

		return info;
	}

	/**
	 * Test if one instruction dominates the other
	 *
	 * @param dominator The PC of the dominator
	 * @param test      The PC to test against
	 * @return If this instruction dominates another
	 */
	public boolean instructionDominates(int dominator, int test) {
		if (dominator == test) return true;
		BasicBlock domBlock = blocks[dominator];
		BasicBlock testBlock = blocks[test];

		// If they are in the same block, check if it occurs before
		if (domBlock == testBlock) return dominator < test;

		// Otherwise, check that the block dominates the other
		return domBlock.dominates(testBlock);
	}
}
