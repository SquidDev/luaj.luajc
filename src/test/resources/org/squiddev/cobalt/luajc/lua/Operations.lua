local a2, a3, aTrue, aFoo, aTable = (function() return 2, 3, true, "foo", {} end)()

assertEquals(2, a2)
assertEquals(3, a3)
assertEquals(true, aTrue)
assertEquals("foo", aFoo)
assertEquals("table", type(aTable))

assertEquals(5, a2 + a3)
assertEquals(-1, a2 - a3)
assertEquals(6, a2 * a3)
assertEquals(2 / 3, a2 / a3)
assertEquals(2, a2 % a3)
assertEquals(8, a2 ^ a3)

assertEquals(-2, -a2)
assertEquals(-3, -a3)

assertEquals(false, not a2)
assertEquals(false, not aTrue)

assertEquals(3, #aFoo)
assertEquals(0, #aTable)

