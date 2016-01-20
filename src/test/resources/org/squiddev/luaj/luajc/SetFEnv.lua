local function foo()
	return x
end

setfenv(foo, setmetatable({ x = 10 }, { __index = getfenv() }))

assertEquals(10, foo())
