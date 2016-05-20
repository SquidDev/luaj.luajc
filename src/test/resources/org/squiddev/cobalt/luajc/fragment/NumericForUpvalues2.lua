local function execute()
	local t = {}
	local template = [[123 456]]
	for i = 1, 2 do
		t[i] = template:gsub('%d', function(s)
			return i
		end)
	end
	return t[2]
end

assertEquals("222 222", execute())
