local function execute()
	local a = foo or 0
	local function b(c)
		if c > a then a = c end
		return a
	end

	b(6)
	return a
end

assertEquals(6, execute())
