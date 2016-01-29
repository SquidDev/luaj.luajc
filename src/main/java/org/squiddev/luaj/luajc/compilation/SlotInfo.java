package org.squiddev.luaj.luajc.compilation;

import org.squiddev.luaj.luajc.analysis.VarInfo;

/**
 * Storage for various slots
 */
public final class SlotInfo {
	public VarInfo lastBoolean;
	public VarInfo lastNumber;
	public VarInfo lastValue;

	public int booleanSlot = -1;
	public int numberSlot = -1;
	public int valueSlot = -1;
}
