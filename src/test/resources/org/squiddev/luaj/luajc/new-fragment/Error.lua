-- Test errors work, as do stack traces
local success, msg = pcall(error, "Kuput!", 2)
assertEquals("new-fragment/Error.lua:2: Kuput!", msg)