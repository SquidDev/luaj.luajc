-- Tests that `string.dump` works on Java functions

local function testing()
	print("HELLO")
end

print(string.dump(testing))
