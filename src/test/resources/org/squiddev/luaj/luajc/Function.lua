-- Assert that sub-functions work
local function add(a, b)
	return a + b
end

assertEquals(2, add(1, 1))
assertEquals(3, add(2, 1))
assertEquals(3, add("2", 1))
