local function execute()
	local aliases = { a = 'b' }
	local foo = function()
		return aliases
	end
	return foo().a
end

assertEquals("b", execute())
