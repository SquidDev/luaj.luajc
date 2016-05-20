local function execute()
	local state
	if _G then
		state = 333
	end
	return state
end

assertEquals(333, execute())
