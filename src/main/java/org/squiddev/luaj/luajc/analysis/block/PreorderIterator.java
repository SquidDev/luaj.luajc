package org.squiddev.luaj.luajc.analysis.block;

import java.util.*;

/**
 * Iterator for
 */
public class PreorderIterator implements Iterator<BasicBlock> {
	private final Deque<BasicBlock> toVisit = new ArrayDeque<BasicBlock>();
	public final Set<BasicBlock> visited = new HashSet<BasicBlock>();

	public PreorderIterator(BasicBlock first) {
		toVisit.push(first);
		hasNext = doNext();
	}

	private BasicBlock next;
	private boolean hasNext;


	@Override
	public boolean hasNext() {
		return hasNext || (hasNext = doNext());
	}

	@Override
	public BasicBlock next() {
		if (!hasNext && !hasNext()) {
			throw new IllegalStateException("No remaining items");
		}

		hasNext = false;
		return next;
	}

	private boolean doNext() {
		if (next != null) {
			for (BasicBlock child : next.next) {
				toVisit.push(child);
			}
			next = null;
		}

		while (!toVisit.isEmpty()) {
			BasicBlock item = toVisit.pop();
			if (visited.add(item)) {
				next = item;
				return true;
			}
		}

		return false;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove");
	}
}
