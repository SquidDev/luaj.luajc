local function execute()
	local a = '\a\b\f\n\t\v\133\222'
	local t = { string.byte(a, 1, #a) }
	return table.concat(t, ',')
end

assertEquals("7,8,12,10,9,11,133,222", execute())
