local function getMessage(func)
	local success, err = pcall(func)
	if success then error("Expected failure") end

	return (err:gsub("^lua/FancyMessages%.lua:%d+: ?", ""))
end

local a = false

assertEquals("attempt to perform arithmetic on upvalue 'a' (a boolean value)",
	getMessage(function() return 2 + a end))
assertEquals("attempt to perform arithmetic on upvalue 'a' (a boolean value)",
	getMessage(function() return -a end))
assertEquals("attempt to index upvalue 'a' (a boolean value)",
	getMessage(function() return a.b end))
assertEquals("attempt to index field 'foo' (a nil value)",
	getMessage(function() return _G.foo[a] end))
assertEquals("attempt to call upvalue 'a' (a boolean value)",
	getMessage(function() a() end))
