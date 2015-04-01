-- The name "arg" is treated specially, and ends up masking the argument value in 5.1

local function execute()
	function v(arg, ...)
		return type(arg)
	end

	return v('abc')
end

assertEquals("table", execute())

