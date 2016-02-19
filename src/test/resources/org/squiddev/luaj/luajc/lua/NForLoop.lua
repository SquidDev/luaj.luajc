-- Validate loops (and concatination)
local j = ""
for i = 0, 9, 1 do
	j = j .. i
end

assertEquals("0123456789", j)

