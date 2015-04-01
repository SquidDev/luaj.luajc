local function execute()
	local w = ''
	function t()
		for f, var in ipairs({ 'aa', 'bb' }) do
			local s = 234
			w = w .. ' ' .. s .. ',' .. f .. ',' .. var
		end
	end

	t()
	return w
end

assertEquals(" 234,1,aa 234,2,bb", execute())
