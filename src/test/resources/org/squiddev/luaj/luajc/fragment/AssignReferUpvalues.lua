local function execute()
	local entity = 234
	local function c()
		return entity
	end

	entity = (a == b) and 123
	if entity then
		return entity
	end
end

assertEquals(123, execute())
