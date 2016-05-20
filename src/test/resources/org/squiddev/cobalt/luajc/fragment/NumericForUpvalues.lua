local function execute()
	for i = 3, 4 do
		i = i + 5
		local a = function()
			return i
		end
		return a()
	end
end

assertEquals(8, execute())
