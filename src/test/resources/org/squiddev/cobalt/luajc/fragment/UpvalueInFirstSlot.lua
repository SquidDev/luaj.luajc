local function execute()
	local p = { 'foo' }
	bar = function()
		return p
	end
	for i, key in ipairs(p) do
		print()
	end
	return bar()[1]
end

assertEquals("foo", execute())
