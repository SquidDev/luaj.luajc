local function execute()
	local a = 111
	local b = 222
	local c = function()
		a = a + b
		return a, b
	end
	return c()
end

assertMany(333, 222, execute())
