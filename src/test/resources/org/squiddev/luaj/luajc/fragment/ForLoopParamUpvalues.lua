local function execute()
	for n, p in ipairs({ 77 }) do
		print('n,p', n, p)
		foo = function()
			return p, n
		end
		return foo()
	end
end

assertMany(77, 1, execute())
