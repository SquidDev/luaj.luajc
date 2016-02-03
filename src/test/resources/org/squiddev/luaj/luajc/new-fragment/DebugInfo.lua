local function testing()
	local info = debug.getinfo(1)
	assertEquals(2, info.currentline, "currentline")
end

local info = debug.getinfo(testing)

assertEquals(1, info.linedefined, "linedefined")
assertEquals(4, info.lastlinedefined, "lastlinedefined")
assertEquals("new-fragment/DebugInfo.lua", info.short_src, "short_src")
assertEquals("new-fragment/DebugInfo.lua", info.short_src, "short_src")
assertEquals("Lua", info.what, "what")
assertEquals(-1, info.currentline, "currentline")