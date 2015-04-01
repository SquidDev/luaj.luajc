local function execute()
	function q(a, ...)
		return a, arg.n, arg[1], arg[2], arg[3]
	end

	return q('a', 'b', 'c')
end

assertMany("a", 2, "b", "c", nil, execute())
