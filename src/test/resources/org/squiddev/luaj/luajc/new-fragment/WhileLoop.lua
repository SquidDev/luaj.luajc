-- Validate while loops
local a, b = 1, 1
while a < 100 do
	a, b = a + b, a
end

assertEquals(144, a)
