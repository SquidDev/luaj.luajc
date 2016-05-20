local function execute()
	local a = 'a\0b\18c\018d\0180e'
	local t = { string.byte(a, 1, #a) }
	return table.concat(t, ',')
end

assertEquals("97,0,98,18,99,18,100,18,48,101", execute())
