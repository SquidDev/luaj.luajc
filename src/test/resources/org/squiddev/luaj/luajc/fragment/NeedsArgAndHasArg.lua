local function execute()
	function r(q, ...)
		local a = arg
		return a and a[2]
	end

	function s(q, ...)
		local a = arg
		local b = ...
		return a and a[2], b
	end

	return r(111, 222, 333), s(111, 222, 333)
end

assertMany(333, nil, 222, execute())
