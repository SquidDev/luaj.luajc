package org.squiddev.luaj.luajc.analysis.type;

import org.squiddev.luaj.luajc.utils.IntArray;

/**
 * Extended information about types
 */
public class TypeInfo {
	/**
	 * The type of this variable
	 */
	public final BasicType type;

	/**
	 * Places where the generic type is defined
	 */
	public final IntArray[] definitions;

	public int useFlag = 0;

	public TypeInfo(BasicType type) {
		this.type = type;
		definitions = new IntArray[3];
		for (int i = 0; i < 3; i++) definitions[i] = new IntArray();
	}

}
