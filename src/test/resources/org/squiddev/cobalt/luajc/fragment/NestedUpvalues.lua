local function execute()
	local x = 3
	local y = 5
	local function f()
		return y
	end

	local function g(x1, y1)
		x = x1
		y = y1
		return x, y
	end

	return f(), g(8, 9)
end

assertMany(5, 8, 9, execute())
