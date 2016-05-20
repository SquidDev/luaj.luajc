local function execute()
	local iter = function() return 1, 2, 3, 4 end
	local foo = function() return iter, 5 end
	for a, b, c in foo() do
		return c, b, a
	end
end

assertMany(3, 2, 1, execute())
