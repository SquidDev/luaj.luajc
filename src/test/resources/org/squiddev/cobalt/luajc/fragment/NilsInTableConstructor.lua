local function execute()
	local t = { 111, 222, 333, nil, nil }
	local s = ''
	for i, v in ipairs(t) do
		s = s .. tostring(i) .. '=' .. tostring(v) .. ' '
	end
	return s
end

assertEquals("1=111 2=222 3=333 ", execute())
