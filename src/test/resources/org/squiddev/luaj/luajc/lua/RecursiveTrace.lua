-- Checks that stacks are calculated correctly

local function assertLine(stack, line)
	local success, msg = pcall(error, "", stack + 2)
	assertEquals("RecursiveTrace.lua:" .. line .. ": ", msg)
end

local function assertStack()
	assertLine(1, 9)
	assertLine(2, 18)
	assertLine(3, 20)
	assertLine(4, 24)
end

local func
func = function(verify)
	if verify then
		assertStack()
	else
		func(true)
	end
end

func(false)
