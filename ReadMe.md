# LuaJC [![Build Status](https://travis-ci.org/SquidDev/luaj.luajc.svg?branch=cobalt)](https://travis-ci.org/SquidDev/luaj.luajc)
LuaJ, but faster

This is a fork of LuaJ's Lua to Java bytecode compiler. It has been
converted to use the ASM framework and many bugs have been fixed.

## Changes from the original
 - Core debug support (`debug.traceback`, `debug.getinfo` and all debug hooks - you cannot get or change locals though)
 - `string.dump` support
 - `getfenv` and `setfenv` support
 - Delayed compilation: only compile after n calls.
 - Fixes several bugs with generation (see `BranchUpvalue2`, `EdgeCases` and `NilCallReturn`)

## Performance
Testing on [this aes implementation](https://github.com/SquidDev-CC/aeslua) produces these results:

Task        | LuaJC    | LuaC      |
------------|---------:|----------:|
Compilation | 0.019550 |  0.031101 |
Running     | 8.512504 | 20.131379 |

## Getting started
Firstly add this as a dependency:

```groovy
repositories {
	maven {
		name = "squiddev"
		url = "https://dl.bintray.com/squiddev/maven"
	}
}

dependencies {
	compile 'org.squiddev:cobalt.luajc:1.+'
	compile 'org.squiddev:cobalt:0.2'
}
```

Then install it as a compiler after setting up the globals.

```java
import org.squiddev.cobalt.luajc.LuaJC;

public LuaValue load(InputStream stream, String name) throws IOException {
	LuaState state = new LuaState(new FileResourceManipulator());
	LuaTable globals = JsePlatform.debugGlobals(state);
	LuaJC.install(state);
	return state.compiler.load(stream, name, globals);
}
```

You can also pass an instance of the `CompileOptions` class when installing the compiler.
