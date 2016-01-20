package org.squiddev.luaj.luajc;

import org.luaj.vm2.Prototype;

/**
 * Interface used to get current debugging info
 */
public interface IGetSource {
	int getCurrentLine();

	Prototype getPrototype();
}
