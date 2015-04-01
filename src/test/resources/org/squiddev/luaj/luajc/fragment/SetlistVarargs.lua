local function execute()
	local f = function() return 'abc' end
	local g = { f() }
	return g[1]
end

assertEquals("abc", execute())
