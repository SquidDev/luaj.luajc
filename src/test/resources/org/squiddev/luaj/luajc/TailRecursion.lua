local function fact(n, current)
	local current = current or 0
	if n == 0 then
		return current
	else
		return fact(n - 1, current * n)
	end
end

fact(300, 1)

assertEquals(3628800, fact(10, 1))
