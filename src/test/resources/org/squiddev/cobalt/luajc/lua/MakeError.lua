local function throw()
	makeError("hello")
end

local success, message = pcall(throw)
assertEquals(false, success, "Should have errored")
assertEquals("lua/MakeError.lua:2: vm error: java.lang.RuntimeException: hello", message)

local function throw()
	error("hello")
end

local success, message = pcall(throw)
assertEquals(false, success, "Should have errored")
assertEquals("lua/MakeError.lua:10: hello", message)
