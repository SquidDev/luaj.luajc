package org.squiddev.luaj.luajc.luaj;

import org.luaj.vm2.*;

/**
 * Helper conversions
 */
public class Conversion {
	/**
	 * Convert a string to a LuaValue. Supports nulls
	 *
	 * @param value The value to convert
	 * @return The resulting LuaValue
	 */
	public static LuaValue valueOf(String value) {
		if (value == null) return LuaValue.NIL;
		return LuaString.valueOf(value);
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(String[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = valueOf(items[i]);
		}

		return result;
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(int[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = LuaInteger.valueOf(items[i]);
		}

		return result;
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(short[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = LuaInteger.valueOf(items[i]);
		}

		return result;
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(byte[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = LuaInteger.valueOf(items[i]);
		}

		return result;
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(char[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = LuaInteger.valueOf(items[i]);
		}

		return result;
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(float[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = LuaDouble.valueOf(items[i]);
		}

		return result;
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(double[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = LuaDouble.valueOf(items[i]);
		}

		return result;
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(long[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = LuaInteger.valueOf(items[i]);
		}

		return result;
	}

	/**
	 * Convert an array of values to LuaValues
	 *
	 * @param items The items to convert
	 * @return The resulting array
	 */
	public static LuaValue[] valueOf(boolean[] items) {
		int l = items.length;
		LuaValue[] result = new LuaValue[l];

		for (int i = 0; i < l; i++) {
			result[i] = LuaBoolean.valueOf(items[i]);
		}

		return result;
	}
}
