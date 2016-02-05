local function numbers()
	return 1, 2, 3
end

local function variableCall()
	numbers(nil, numbers())
end

local function variableReturn()
	return nil, numbers()
end

local function anotherReturn()
	return nil, 1, 2
end

variableCall()
assertMany(nil, 1, 2, 3, variableReturn())
assertMany(nil, 1, 2, anotherReturn())
