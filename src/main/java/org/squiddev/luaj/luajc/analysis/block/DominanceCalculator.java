package org.squiddev.luaj.luajc.analysis.block;

import org.squiddev.luaj.luajc.analysis.ProtoInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Calculate the dominance frontier for the blocks
 */
public final class DominanceCalculator {
	public static void calculate(ProtoInfo info) {
		BasicBlock root = info.blockList[0];
		root.dominator = root;

		boolean changed;
		do {
			changed = false;

			PreorderIterator iterator = new PreorderIterator(root);
			Set<BasicBlock> visited = iterator.visited;
			while (iterator.hasNext()) {
				BasicBlock node = iterator.next();
				if (node == root) continue;

				BasicBlock newIdiom = null;
				for (BasicBlock previous : node.prev) {
					if (visited.contains(previous) && previous != node) {
						newIdiom = previous;
						break;
					}
				}

				for (BasicBlock previous : node.prev) {
					if (previous != newIdiom && previous.dominator != null) {
						newIdiom = findCommonDominator(previous, newIdiom);
					}
				}

				if (newIdiom != node.dominator) {
					node.dominator = newIdiom;
					changed = true;
				}
			}

		} while (changed);
	}

	private static BasicBlock findCommonDominator(BasicBlock x, BasicBlock y) {
		HashSet<BasicBlock> path = new HashSet<BasicBlock>();

		while (x != null && path.add(x)) {
			x = x.dominator;
		}
		while (y != null) {
			if (path.contains(y)) {
				return y;
			} else {
				y = y.dominator;
			}
		}

		// This should never happen
		throw new IllegalStateException("No common dominator found");
	}
}
