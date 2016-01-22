--[[
	Time spent on this bug: 4 hours

	The issue lies in UpvalueInfo. The code believes that the second for loop writes to upvalue
	as its phi values do not have an upvalue, and so it must be a write boundry.

	0.11 (second for prep) and 0.15 (function call) do not have an upvalue and are phis of {0.0, 0.5} but 0.18
	(second for loop) does.
 ]]

local upvalue = 1

local function closure()
	return upvalue
end

while true do
	if 1 == 2 then
		upvalue = 1
	else
		for loop_numA = 1, 2 do
			for loop_numB = 1, 2 do
				closure(loop_numA + loop_numB)
			end
		end

		break
	end
end
