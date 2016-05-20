package org.squiddev.cobalt.luajc.utils;

import java.util.Arrays;

/**
 * Primitive implementation of int lists
 */
public final class IntArray {
	private static final int DEFAULT_SIZE = 5;
	private static final int[] DEFAULT_DATA = {};

	private int[] values = DEFAULT_DATA;
	private int size = 0;

	public int size() {
		return size;
	}

	public int[] values() {
		return values;
	}

	public void add(int item) {
		ensureCapacity(size + 1);
		values[size++] = item;
	}

	public void add(IntArray item) {
		ensureCapacity(size + item.size());
		int otherSize = item.size;
		int[] otherValues = item.values;

		int size = this.size;
		int[] values = this.values;
		for (int i = 0; i < otherSize; i++) {
			values[size++] = otherValues[i];
		}

		this.size = size;
	}

	public void ensureCapacity(int capacity) {
		if (values == DEFAULT_DATA && capacity < DEFAULT_SIZE) {
			capacity = DEFAULT_SIZE;
		}

		if (capacity > values.length) grow(capacity);
	}

	private void grow(int minCapacity) {
		int oldCapacity = values.length;
		int newCapacity = oldCapacity + (oldCapacity >> 1);

		if (newCapacity - minCapacity < 0) newCapacity = minCapacity;
		values = Arrays.copyOf(values, newCapacity);
	}

	public boolean contains(int value) {
		int[] values = this.values;
		int size = size();
		for (int i = 0; i < size; i++) {
			if (values[i] == value) return true;
		}

		return false;
	}

	public void set(int index, int value) {
		values[index] = value;
	}

	public int get(int index) {
		return values[index];
	}

	public int[] toArray() {
		return Arrays.copyOf(values, size);
	}

	@Override
	public String toString() {
		return Arrays.toString(values);
	}
}
