local function execute()
	function r(a, ...)
		return a, type(arg), ...
	end

	return r('a', 'b', 'c')
end

assertMany("a", "nil", "b", "c", execute())
