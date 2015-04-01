local function execute()
	local a = 1
	local function b()
		a = a + 1
		return function() end
	end

	for i in b() do
		a = 3
	end
	return a
end

assertEquals(2, execute())
