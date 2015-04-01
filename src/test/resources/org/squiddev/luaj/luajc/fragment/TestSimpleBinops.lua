local function execute()
	local a, b, c = 2, -2.5, 0
	return (a == c), (b == c), (a == a), (a > c), (b > 0)
end

assertMany(false, false, true, true, false, execute())
