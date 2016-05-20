local function execute()
	if a then
		foo(bar)
	elseif _G then
		local x = 111
		if d then
			foo(bar)
		else
			local y = function()
				return x
			end
			return y()
		end
	end
end

assertEquals(111, execute())
