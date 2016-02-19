-- Check scoping
local a = "Hello"
do
	local a = "Thing"
end

assertEquals("Hello", a)
