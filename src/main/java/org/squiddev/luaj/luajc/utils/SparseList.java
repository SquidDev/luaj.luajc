package org.squiddev.luaj.luajc.utils;

import java.util.ArrayList;

/**
 * A list that can cope with being set beyond its range.
 */
public class SparseList<T> extends ArrayList<T> {
	@Override
	public T set(int index, T value) {
		int size = size();
		if (size <= index) {
			ensureCapacity(index + 1);
			for (int i = size(); i < index + 1; i++) {
				add(null);
			}
		}

		return super.set(index, value);
	}

	@Override
	public T get(int index) {
		return index < size() ? super.get(index) : null;
	}
}
