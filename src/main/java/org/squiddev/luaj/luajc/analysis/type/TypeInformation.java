package org.squiddev.luaj.luajc.analysis.type;

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
	public boolean usesSpecialised = false;

	/**
	 * If the {@link org.luaj.vm2.LuaValue} representation fo thie variable is used.
	 */
	public boolean usesValue = false;

	public TypeInformation(BasicType type) {
		this.type = type;
	}
}
