-- Upvalues near branches were buggy

-- The upvalue creation is injected before a label, so is never executed under some cicumstances,
-- an so compilation fails as the slot is empty
local a = { native = function() end }
local upvalue = (a.native and a.native()) or a

-- We create a closure
local closure = function(target)
	print(upvalue)
end

-- Extra branch statements seem to force this to happen. This is probably something to do with Java
-- classes rather than the builder
while false do end
