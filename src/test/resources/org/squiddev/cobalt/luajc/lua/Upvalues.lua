-- Upvalues
local a = "Hello"
local function increase()
	if a == "Hello" then
		a = 1
	else
		a = a + 1
	end
end

increase()
assertEquals(1, a)

increase()
assertEquals(2, a)
