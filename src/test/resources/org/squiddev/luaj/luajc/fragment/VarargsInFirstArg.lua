local function execute()
	function aaa(x) return x end

	function bbb(y) return y end

	function ccc(z) return z end

	return ccc(aaa(bbb(123)), aaa(456))
end

assertEquals(123, execute())
