local function execute()
	local testtable = {}
	return pcall(function() testtable[1] = 2 end)
end

assertEquals(true, execute())
