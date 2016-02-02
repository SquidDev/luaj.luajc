local function add(x, y, step)
	x = x or 0
	y = y or 0
	if step then
		x = x + 1
	end

	return x + "2", x + y
end

add(1, 2, false)
add(1, 2, false)
add(1, 2, false)
add(1, 2, false)
add(nil, 2, false)
add(nil, 2, false)
add(nil, 2, false)
add(nil, 2, false)
add(2, nil, false)
add(2, nil, false)
add(2, nil, false)
add(2, nil, false)
