-- Tests tail calls

local function numbers()
	return 1, 2, 3
end

local function tail()
	return nil, "b", numbers()
end

local a, b, c, d, e = tail()
assertEquals(nil, a)
assertEquals("b", b)
assertEquals(1, c)
assertEquals(2, d)
assertEquals(3, e)
