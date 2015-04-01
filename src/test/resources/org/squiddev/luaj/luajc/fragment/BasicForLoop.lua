local function execute()
	local data
	for i = 1, 2 do
		data = i
	end
	local bar = function()
		return data
	end
	return bar()
end

assertEquals(2, execute())
