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

import org.luaj.vm2.LuaValue;
import org.squiddev.luaj.luajc.analysis.block.BasicBlock;
import org.squiddev.luaj.luajc.utils.IntArray;

import java.util.Set;

public class VarInfo {
	/**
	 * A VarInfo that has not been assigned to yet
	 */
	public static final VarInfo INVALID = new VarInfo(-1, -1);

	/**
	 * Create a {@link VarInfo} for a parameter
	 *
	 * @param slot The slot this parameter is in
	 * @return The resulting parameter variable. The PC is set to -1
	 */
	public static VarInfo param(int slot) {
		return new VarInfo(slot, -1) {
			public String toString() {
				return slot + ".p";
			}
		};
	}

	// Counts for value tracking
	public int booleanCount = 0;
	public int numberCount = 0;
	public int valueCount = 0;

	/**
	 * The slot this variable exists in
	 */
	public final int slot;

	/**
	 * The PC this variable is written at
	 * -1 for a block inputs
	 */
	public final int pc; // where assigned, or -1 if for block inputs

	/**
	 * The upvalue info
	 * Null if this is not an upvalue
	 */
	public UpvalueInfo upvalue;

	/**
	 * If this variable allocates read/write upvalue storage
	 */
	public boolean allocUpvalue;

	/**
	 * If this variable is referenced
	 */
	public boolean isReferenced;

	/**
	 * List of instructions that reference this
	 */
	public final IntArray references = new IntArray();

	/**
	 * List of references including through phi nodes
	 */
	public final IntArray phiReferences = new IntArray();

	public VarInfo(int slot, int pc) {
		this.slot = slot;
		this.pc = pc;
	}

	public String toString() {
		return slot < 0 ? "x.x" : (slot + "." + pc);
	}

	/**
	 * Return replacement variable if there is exactly one value possible,
	 * otherwise compute entire collection of variables and return null.
	 * Computes the list of all variable values, and saves it for the future.
	 *
	 * @return new Variable to replace with if there is only one value, or null to leave alone.
	 */
	public VarInfo resolvePhiVariableValues() {
		return null;
	}

	/**
	 * Used to create unique variables
	 *
	 * @param visitedBlocks The list of blocks already visited
	 * @param vars          The list of unique variables
	 */
	protected void collectUniqueValues(Set<BasicBlock> visitedBlocks, Set<VarInfo> vars) {
		vars.add(this);
	}

	/**
	 * Is this variable a {@link PhiInfo}
	 *
	 * @return If it is a Phi var
	 */
	public boolean isPhiVar() {
		return false;
	}

	/**
	 * Check if this variable contains a definition of another
	 *
	 * @param other The variable to check
	 * @return If this variable contains another
	 */
	public boolean contains(VarInfo other) {
		return this == other;
	}

	/**
	 * Increment this variable with a particular type
	 *
	 * @param value The value to typecheck
	 */
	public final void increment(LuaValue value) {
		switch (value.type()) {
			case LuaValue.TBOOLEAN:
				booleanCount++;
				break;
			case LuaValue.TNUMBER:
				numberCount++;
				break;
			default:
				valueCount++;
				break;
		}
	}

	/**
	 * Mark this variable as being referenced
	 *
	 * @param pc The PC that references this
	 */
	public final void reference(int pc) {
		isReferenced = true;
		references.add(pc);
		phiReferences.add(pc);
	}
}
