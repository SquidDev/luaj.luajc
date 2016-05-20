local function execute()
	local function foo(x) return x * 2 end

	local function bar(x, y)
		if x == y then
			return y
		else
			return foo(x)
		end
	end

	return bar(33, 44)
end

assertEquals(66, execute())
