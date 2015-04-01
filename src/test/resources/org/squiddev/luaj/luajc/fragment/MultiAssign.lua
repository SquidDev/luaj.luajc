-- array args evaluations are all done before assignments

local function execute()
	a, b, c = 1, 10, 100
	a, b, c = a + b + c, a + b + c, a + b + c
	return a, b, c
end

assertMany(111, 111, 111, execute())
