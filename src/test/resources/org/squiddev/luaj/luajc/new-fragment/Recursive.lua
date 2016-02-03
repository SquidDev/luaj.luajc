-- Basic recursive functions and upvalues
local factorial
factorial = function(a)
	if a == 1 then return 1 end
	return a * factorial(a - 1)
end

assertEquals(720, factorial(6))
