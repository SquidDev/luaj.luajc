-- Tests tail calls

local function numbers()
	return 2, 3
end

local function tail()
	return 1, numbers()
end

local a, b, c = tail()
assertEquals(1, a)
assertEquals(2, b)
assertEquals(3, c)
