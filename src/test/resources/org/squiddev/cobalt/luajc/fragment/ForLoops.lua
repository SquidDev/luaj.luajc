local function execute()
	local s, t, u = '', '', ''
	for m = 1, 5 do
		s = s .. m
	end
	for m = 3, 7, 2 do
		t = t .. m
	end
	for m = 9, 3, -3 do
		u = u .. m
	end
	return s .. ' ' .. t .. ' ' .. u
end

assertEquals("12345 357 963", execute())
