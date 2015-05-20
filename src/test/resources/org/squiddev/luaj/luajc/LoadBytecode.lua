-- Tests that `loadstring(bytecode)` works

local function testing()
	return "HELLO"
end

assertEquals("HELLO", loadstring(string.dump(testing))())
