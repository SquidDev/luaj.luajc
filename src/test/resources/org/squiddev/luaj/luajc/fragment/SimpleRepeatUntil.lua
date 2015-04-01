local function execute()
	local a
	local w
	repeat
		a = w
	until not a
	return 5
end

assertEquals(5, execute())
