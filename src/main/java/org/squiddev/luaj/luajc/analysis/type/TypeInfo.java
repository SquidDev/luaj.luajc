package org.squiddev.luaj.luajc.analysis.type;

/**
 * Extended information about types
 */
public class TypeInfo {
	/**
	 * The type of this variable
	 */
	public final BasicType type;

	/**
	 * If the generic value is ever referenced
	 */
	public boolean valueReferenced = false;

	/**
	 * If the specialist value is ever referenced
	 */
	public boolean specialisedReferenced = false;

	/**
	 * If the value type will be available (either at creation of due to usage)
	 */
	public boolean valueAvailable = false;

	public TypeInfo(BasicType type) {
		this.type = type;
	}

	public void referenceValue(int pc) {
		valueReferenced = true;
		valueAvailable = true;
	}

	public void referenceSpecialised(int pc) {
		assert type != BasicType.VALUE : "Value is not a specialised type";
		specialisedReferenced = true;
	}

	@Override
	public String toString() {
		String base = type.format() + (valueAvailable ? "v" : "s");
		if (specialisedReferenced && valueReferenced) {
			return base + "b";
		} else if (specialisedReferenced) {
			return base + "s";
		} else if (valueReferenced) {
			return base + "v";
		} else {
			return base + " ";
		}
	}

	public void absorb(TypeInfo info) {
		valueReferenced |= info.valueReferenced;
		if (info.type == type) {
			specialisedReferenced |= info.specialisedReferenced;
		}
	}
}
