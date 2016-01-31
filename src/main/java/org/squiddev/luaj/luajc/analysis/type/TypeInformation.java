package org.squiddev.luaj.luajc.analysis.type;

import org.squiddev.luaj.luajc.utils.IntArray;

/**
 * Type information about a variable
 */
public class TypeInformation {
	/**
	 * The type this variable is
	 */
	public final BasicType type;
	/**
	 * If the specialised type is used
	 */
	public IntArray usesSpecialised = new IntArray();

	/**
	 * If the {@link org.luaj.vm2.LuaValue} representation fo this variable is used.
	 */
	public IntArray usesValue = new IntArray();

	public TypeInformation(BasicType type) {
		this.type = type;
	}
}
