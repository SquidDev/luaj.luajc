local function foo(x, y)
	return function(z)
		y = y + 1
		return x + y + z
	end
end

assertEquals(10, foo(1, 5)(3))
