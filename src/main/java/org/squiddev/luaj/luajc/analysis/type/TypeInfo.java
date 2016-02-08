package org.squiddev.luaj.luajc.analysis.type;

/**
 * Extended information about types
 */
public class TypeInfo {
	/**
	 * The type of this variable
	 */
	public final BasicType type;

	public boolean valueReferenced = false;

	public boolean specialisedReferenced = false;

	public TypeInfo(BasicType type) {
		this.type = type;
	}

	public void referenceValue(int pc) {
		valueReferenced = true;
	}

	public void referenceSpecialised(int pc) {
		assert type != BasicType.VALUE : "Value is not a specialised type";
		specialisedReferenced = true;
	}

	@Override
	public String toString() {
		if (specialisedReferenced && valueReferenced) {
			return type.format() + "b";
		} else if (specialisedReferenced) {
			return type.format() + "s";
		} else if (valueReferenced) {
			return type.format() + "v";
		} else {
			return type.format() + " ";
		}
	}

	public void absorb(TypeInfo info) {
		valueReferenced |= info.valueReferenced;
		specialisedReferenced |= info.specialisedReferenced;
	}
}
