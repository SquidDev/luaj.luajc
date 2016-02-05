# LuaJC [![Build Status](https://travis-ci.org/SquidDev/luaj.luajc.svg?branch=master)](https://travis-ci.org/SquidDev/luaj.luajc)
LuaJ, but faster

This is a fork of LuaJ's Lua to Java bytecode compiler. It has been
converted to use the ASM framework and many bugs have been fixed.

## Changes from the original
 - Core debug support (`debug.traceback`, `debug.getinfo` and all debug hooks - you cannot get or change locals/upvalues though)
 - `string.dump` support
 - `getfenv` and `setfenv` support
 - Delayed compilation: only compile after n calls.
 - Fixes several bugs with generation (see `BranchUpvalue2`, `EdgeCases` and `NilCallReturn`) 
 
## Performance
It is tricky to compare the default `LuaClosure` and `LuaJC` implementations as several optimisations are applied for
debug hooks. These could also be applied to the default VM implementation and increase its performance too.

However there are still some performance increases. 
Testing on [this aes implementation](https://github.com/SquidDev-CC/aeslua) produces these results:

Task        | LuaJC    | LuaC      | Optimised LuaVM
------------|---------:|----------:|--------------:
Compilation | 0.007993 |  0.004938 | 0.004938
Running     | 9.783356 | 16.707443 | 14.739549

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
	compile 'org.squiddev:luaj.luajc:1.+'
}
```

Then install it as a compiler after setting up the globals.

```java
import org.squiddev.luaj.luajc.LuaJC;

public LuaValue load(InputStream stream, String name) throws IOException {
	LuaValue globals = JsePlatform.debugGlobals();
	LuaC.install();
	return LoadState.load(stream, name, globals);
}
```

You can also pass an instance of the `CompileOptions` class when installing the compiler.
