local function execute()
	local a = 1
	local b
	function c()
		b = 5
		return a
	end

	return c(), b
end

assertMany(1, 5, execute())
