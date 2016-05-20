local function execute()
	function v(arg, ...)
		return arg, ...
	end

	return v('a', 'b', 'c')
end

assertMany(nil, "b", "c", execute())
