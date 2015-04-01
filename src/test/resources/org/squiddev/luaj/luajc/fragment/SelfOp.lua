local function execute()
	local s = 'abcde'
	return s:sub(2, 4)
end

assertEquals("bcd", execute())
