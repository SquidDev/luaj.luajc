package org.squiddev.luaj.luajc.analysis.type;

/**
 * Extended information about types
 */
public class TypeInfo {
	/**
	 * The type of this variable
	 */
	public final BasicType type;

	private boolean valueReferenced = false;

	private boolean specialisedReferenced = false;

	public TypeInfo(BasicType type) {
		this.type = type;
	}

	public void referenceValue(int pc) {
		valueReferenced = true;
	}

	public void referenceSpecialised(int pc) {
		specialisedReferenced = true;
	}

	@Override
	public String toString() {
		return type.format() + "(" +
			(specialisedReferenced ? "S" : "") +
			(valueReferenced ? "V" : "") + ")";
	}

	public void absorb(TypeInfo info) {
		valueReferenced |= info.valueReferenced;
		specialisedReferenced |= info.specialisedReferenced;
	}
}
