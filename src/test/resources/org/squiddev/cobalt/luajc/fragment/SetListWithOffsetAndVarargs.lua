local function execute()
	local bar = { 1000, math.sqrt(9) }
	return bar[1] + bar[2]
end

assertEquals(1003, execute())
