-- Edge case when a parameter assignment occurs after a non-returning branch statement in a VarArgsFunction subclass

local function func(a, b, ...)
	if a then error() end
	b = b + 1
end

local function func(a, b, ...)
	if a then b = 0 end
	b = b + 1
end
