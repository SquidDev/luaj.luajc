local function execute()
	print(nil and 'T' or 'F')
	local a, b, c = 1, 2, 3
	function foo()
		return a, b, c
	end

	return foo()
end

assertMany(1, 2, 3, execute())
