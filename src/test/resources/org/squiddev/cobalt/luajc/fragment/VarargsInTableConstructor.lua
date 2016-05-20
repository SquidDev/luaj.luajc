local function execute()
	local function foo() return 111, 222, 333 end

	local t = { 'a', 'b', c = 'c', foo() }
	return t[4]
end

assertEquals(222, execute())
