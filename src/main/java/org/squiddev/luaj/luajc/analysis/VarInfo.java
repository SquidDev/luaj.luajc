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

import java.util.HashSet;
import java.util.Iterator;
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

	/**
	 * Create a new {@link VarInfo.PhiVarInfo}
	 *
	 * @param pi   The prototype this variable is part of
	 * @param slot The slot the variable is assigned to
	 * @param pc   The PC this variable is used at
	 * @return The resulting variable
	 */
	public static VarInfo phi(final ProtoInfo pi, final int slot, final int pc) {
		return new PhiVarInfo(pi, slot, pc);
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
	 * Is this variable a {@link PhiVarInfo}
	 *
	 * @return If it is a Phi var
	 */
	public boolean isPhiVar() {
		return false;
	}

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
	 * This stores a list of potential variables this could resolve to
	 * This is created as a result of multiple blocks
	 */
	private static final class PhiVarInfo extends VarInfo {
		private final ProtoInfo pi;

		/**
		 * A list of variables this could resolve to
		 */
		protected VarInfo[] values;

		private PhiVarInfo(ProtoInfo pi, int slot, int pc) {
			super(slot, pc);
			this.pi = pi;
		}

		@Override
		public boolean isPhiVar() {
			return true;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(super.toString()).append("={");
			for (int i = 0, n = (values != null ? values.length : 0); i < n; i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(String.valueOf(values[i]));
			}
			sb.append("}");
			return sb.toString();
		}

		/**
		 * Return replacement variable if there is exactly one value possible,
		 * otherwise compute entire collection of variables and return null.
		 * Computes the list of all variable values, and saves it for the future.
		 * <p>
		 * This will set all child variables' isReferenced if this variable is
		 *
		 * @return new Variable to replace with if there is only one value, or null to leave alone.
		 * @see #isReferenced
		 */
		@Override
		public VarInfo resolvePhiVariableValues() {
			Set<BasicBlock> visitedBlocks = new HashSet<BasicBlock>();
			Set<VarInfo> vars = new HashSet<VarInfo>();
			collectUniqueValues(visitedBlocks, vars);

			if (vars.contains(INVALID)) return INVALID;

			int n = vars.size();
			Iterator<VarInfo> it = vars.iterator();
			if (n == 1) {
				VarInfo v = it.next();
				v.isReferenced |= isReferenced;
				return v;
			}
			values = new VarInfo[n];
			for (int i = 0; i < n; i++) {
				values[i] = it.next();
				values[i].isReferenced |= isReferenced;
			}
			return null;
		}

		/**
		 * Used to create unique variables
		 *
		 * @param visitedBlocks The list of blocks already visited
		 * @param vars          The list of unique variables
		 */
		@Override
		protected void collectUniqueValues(Set<BasicBlock> visitedBlocks, Set<VarInfo> vars) {
			BasicBlock b = pi.blocks[pc];
			if (pc == 0) {
				vars.add(pi.params[slot]);
			}
			for (int i = 0, n = b.prev != null ? b.prev.length : 0; i < n; i++) {
				BasicBlock bp = b.prev[i];
				if (visitedBlocks.add(bp)) {
					VarInfo v = pi.vars[bp.pc1][slot];
					if (v != null) {
						v.collectUniqueValues(visitedBlocks, vars);
					}
				}
			}
		}
	}
}
