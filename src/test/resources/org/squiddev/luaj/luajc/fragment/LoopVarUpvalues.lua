local function execute()
	local env = {}
	for a, b in pairs(_G) do
		c = function()
			return b
		end
	end
	local e = env
	local f = { a = 'b' }
	for k, v in pairs(f) do
		return env[k] or v
	end
end

assertEquals("b", execute())
