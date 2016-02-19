-- Test errors work, as do stack traces
local success, msg = pcall(error, "Kuput!", 2)
assertEquals("lua/Error.lua:2: Kuput!", msg)
