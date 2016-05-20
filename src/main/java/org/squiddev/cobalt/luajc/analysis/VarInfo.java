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
package org.squiddev.cobalt.luajc.analysis;

import org.squiddev.cobalt.luajc.analysis.block.BasicBlock;
import org.squiddev.cobalt.luajc.utils.IntArray;

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
				return slot + ".p" + (isReferenced ? " " : "?");
			}
		};
	}

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

	public VarInfo(int slot, int pc) {
		this.slot = slot;
		this.pc = pc;
	}

	public String toString() {
		if (slot < 0) {
			return "x.x ";
		} else {
			return slot + "." + pc + (isReferenced ? " " : "?");
		}
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

	public void reference(int pc) {
		isReferenced = true;
		references.add(pc);
	}

	/**
	 * Check if this variable is a reference to a read/write upvalue
	 *
	 * @return If this is a reference to a read/write upvalue
	 */
	public final boolean isUpvalueRefer() {
		return upvalue != null && upvalue.readWrite;
	}

	/**
	 * Check if this variable's creation is an assignment to an upvalue
	 *
	 * @return If an upvalue is assigned to at this point
	 */
	public final boolean isUpvalueAssign() {
		return upvalue != null && upvalue.readWrite;
	}

	/**
	 * Check if this is the creation of an upvalue
	 *
	 * @param pc The current PC
	 * @return If this is where the upvalue is created
	 */
	public final boolean isUpvalueCreate(int pc) {
		return upvalue != null && upvalue.readWrite && allocUpvalue && pc == this.pc;
	}
}
