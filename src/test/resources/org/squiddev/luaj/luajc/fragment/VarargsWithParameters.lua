local function execute()
	local func = function(t, ...)
		return (...)
	end
	return func(111, 222, 333)
end

assertEquals(222, execute())
