local function execute()
	local a = function(x)
		return function(y)
			return x + y
		end
	end
	local b = a(222)
	local c = b(777)
	print('c=', c)
	return c
end

assertEquals(999, execute())
