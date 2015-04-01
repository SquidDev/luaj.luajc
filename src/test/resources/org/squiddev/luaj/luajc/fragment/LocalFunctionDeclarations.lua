local function execute()
	local function aaa()
		return type(aaa)
	end

	local bbb = function()
		return type(bbb)
	end
	return aaa(), bbb()
end

assertMany("function", "nil", execute())
