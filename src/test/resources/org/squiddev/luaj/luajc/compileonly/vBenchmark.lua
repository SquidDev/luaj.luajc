--[[
       ______________________________________
      / | \            |                 |
_    /  |_/  _       _ |_         _      |
 \  /   | \ /_\ +-+ /  | | +-+-+ '_\ ,_  |/
  \/    |_/ \_  | | \_ | | | | | (_| | ' |\


vBenchmark
==========
An advanced benchmarking
program for CC, good for
comparison of various CC
emulators.

version 0.2.3 (or Version (table))
(build number @ /buildn)


Contact me on the
[CC Forums!](http://computercraft.info/forums2)

Made by viluon a.k.a. Andy73
(c) 2015

Dev Notes:
==========
TODO:
--graphs
--DONE:CLI
--Settings
--DONE:organized output (a-z)
--all the tests listed in comments
--DONE:+version in goodbye

+50

]]

local Version = {
	App = "0.2.3",
	DebugID = "REL",
}

local SUICIDAL = false

term.redirect(term.native())
local w, h = term.getSize()

----- startup
term.setBackgroundColor(colors.black)
term.clear()
term.setCursorPos(1, 1)

local function apicheck(n, p, fex) --name, paste ID, File EXtension fix TODO: file extension fix (Test.lua),
if not fs.exists("/" .. (fex and n .. ".lua" or n)) and not _G[n] then
	printError("No " .. n .. " API found, downloading one...")
	shell.run("pastebin get " .. p .. " /" .. (fex and n .. ".lua" or n))
	error()
end
return true
end

apicheck("sha256", "P3KBjR3M")
apicheck("aes", "DMx8M0LP")
apicheck("crc32", "x52gQYWp")
apicheck("json", "jK22eUfn")
apicheck("Test", "wZbpA1Gy", true)

--[[if not fs.exists"/Test.lua" and not _G["Test"] then
  printError"The 'Test' library was not found"
  print"Downloading one..."
  return shell.run"pastebin get wZbpA1Gy /Test.lua"
end]]

local libs = {
	["Test.lua"] = dofile,
	["sha256"] = os.loadAPI,
	["aes"] = os.loadAPI,
	["crc32"] = os.loadAPI,
	["json"] = os.loadAPI,
}
for k, v in pairs(libs) do
	local ok, err = pcall(v, "/" .. k)
	if not ok then print(err) sleep(2) end
end

pcall(fs.delete, "/.vBenchmarkUpdater.lua")

Version.Library = Test.Version

-------- GENERAL FUNCTIONS AND CLASSES
local function s()
	local t = tostring({})
	os.queueEvent(t)
	coroutine.yield(t)
end

local function pairsByKeys(t, f)
	local a = {}
	for n in pairs(t) do table.insert(a, n) end
	table.sort(a, f)
	local i = 0 -- iterator variable
	local iter = function() -- iterator function
	i = i + 1
	if a[i] == nil then return nil
	else return a[i], t[a[i]]
	end
	end
	return iter
end

local RESULT
local names = {
	["ow"] = "Optimized write test",
	["uw"] = "Unoptimized write test",
	["tb"] = "term.blit write test",
	["ct"] = "Color swap/rainbow test",
	["lc"] = "Line clear speed test",
	["cs"] = "Clear speed test",
	["scr"] = "Buffer scroll test",
	["aa"] = "Anti-aliasing test",
	["hsh"] = "SHA256 hash test",
	["aese"] = "AES Encryption perf. test",
	["aesd"] = "AES Decryption perf. test",
	["tec"] = "Table entry creation",
	["ted"] = "Table entry deletion",
	["tea"] = "Table entry access",
	["dhke"] = "Diffie-Hellman key exchange",
	["crc"] = "CRC32 hash test",
	["cmpl1"] = "Simple compilation",
	["cmpl2"] = "Medium compilation",
}
local TaskList = {}
for k, v in pairs(names) do
	TaskList[k] = true
end

local outputPath = "/benchmark.txt"
local maxTime = 2 --maximum b. test time in seconds
local maxCalls = 10000 --maximum b. test iterations (number of calls)




--WARN IMPORTANT ATTENTION
--implement CLI here!!!!!!!!!!!!!!!!!!!!!

local function listToTable(str)
	if type(str) ~= "string" then error("(1)Expected string, got " .. type(str), 2) end
	if str:sub(#str, #str) ~= "," then str = str .. "," end
	local out = {}
	for entry in str:gmatch("(%a+),") do
		table.insert(out, entry)
	end
	return out
end

local function help(c)
	local txt = [[

Welcome to vBenchmark!

  vBenchmark is a complex benchmarking program for ComputerCraft 1.6+, made by viluon (a.k.a. Andy73)

  [Usage]
    vBenchmark -option value --flag -option2 list,of,values



    NOTE: Options and flags are executed in the order they're supplied in

    <required value> [optional value] (name)

    Available options:
    =================
      update (update) updates the program
      =====
      help OR ? OR /? ['light'] (help/usage) displays this page, optionally using black text on white background (this option stops the execution automatically)
      =====
      -o <path> (output) sets the output file to <path>
      =====
      -list [path] (list) lists the names and corresponding codes of available benchmark functions, optionally saving them to [path]
      =====
      -maxtime <seconds> (maximum time limit) sets the time limit per ONE benchmark function to <seconds> - default: 2
      =====
      -maxcalls <calls> (maximum function calls) sets the maximum amount of function calls per ONE benchmark function to <calls> - default: 10 000
      =====
      -exclude <tests> (exclude) excludes a list of tests from the benchmark, <tests> is a list of test codes
      =====
      -testonly <tests> (test only) sets the test list for the benchmark to <tests>
      =====

    Available flags:
    ===============
      --test (test) prints some debug text on the screen
      =====
      --stop (stop) stops the execution of the benchmark
      =====
      --version (version info) displays the version information of the program and used libraries
      =====
      --SUICIDE (suicidal mode) Enables the suicidal mode:
          Suicidal mode is intended for debugging, evaluating and precision testing of CC emulators or SINGLEPLAYER CC. Do NOT use the suicidal benchmark in multiplayer!!! Suicidal benchmark does not yield to produce the most relevant results. As such, it should NOT be used for regular benchmarking. The suicidal mode can corrupt files and do IRREVERSIBLE HARM!!! Use at your own risk.
      =====
      --safe (safe mode) enables safety features:
          -output file backup
          -visible error reports - launches the benchmark in a temporary handler (/.vBenchmarkSafe) that pauses after crashing => errors visible
          -does not intersect with suicidal mode
      ]]
	--TODO -load <paths> load list of custom tests loaded from <paths>
	term.setBackgroundColor(c and colors.white or colors.black)
	term.setTextColor(c and colors.black or colors.white)
	term.clear()
	term.setCursorPos(1, 1)
	textutils.pagedPrint(txt)
end

local Args = { ... }
local args = {}
for k, v in pairs(Args) do
	args[k] = v
end
local flags = {
	["test"] = function()
		print "This is a test"
		sleep(3)
	end,
	["stop"] = function()
		error()
	end,
	["version"] = function()
		print(textutils.serialize(Version))
	end,
	["suicide"] = function()
		SUICIDAL = true
	end,
	["safe"] = function()
		if fs.exists(outputPath) then
			local bkpPath, i = ".bkp1", 2
			while fs.exists(outputPath .. bkpPath) do
				bkpPath = ".bkp" .. i
				i = i + 1
			end
			local ok, err = pcall(fs.copy, outputPath, outputPath .. bkpPath)
			if not ok then
				print "[ERROR] Failed to create backup"
				print(err)
				print "Press ENTER to exit"
				read " "
			end
		end
		local f = fs.open("/.vBenchmarkSafe", "w")
		if not f then print "[ERROR] Failed to open file for writing: /.vBenchmarkSafe" print(e) sleep(5) error() end
		f.write([[
print"Launching vBenchmark in safe mode"
sleep(2)
local path,args="]] .. shell.getRunningProgram() .. [[",{...}
local f=fs.open(path,"r")
local fn=f.readAll()
f.close()
fn=loadstring(fn,"vBenchmark [SAFE MODE]")
if not fn then error("Failed to execute safe mode: file is corrupt!",0) end
setfenv(fn,setmetatable({shell=setmetatable({getRunningProgram=function()return path end},{__index=_G})},{__index=_G}))
local ok,err=pcall(fn,unpack(args)) --line 10
if not ok then
 printError"vBenchmark has crashed."
 print""
 print(err)
 print"Enter to continue..."
 read" "
end
return true

--line 20
]])
		f.close()
		local n = Args
		for k, v in pairs(n) do
			if v:lower() == "--safe" then table.remove(n, k) end
			if #n < 1 or #n == k then break end
		end
		shell.run("/.vBenchmarkSafe", unpack(n))
		error()
	end,
}
local properties = {
	["o"] = {
		function(path)
			if type(path) ~= "string" then error("Expected string as output path, got " .. type(path), 0) end
			outputPath = path
		end,
		1, --max arguments
		1, --min arguments
	},
	["list"] = {
		function(path)
			for k, v in pairs(names) do
				print(v .. "=" .. k)
			end
			if type(path) == "string" then
				if fs.exists(path) then
					local f = fs.open(path, "w")
					f.write(textutils.serialize(names))
					f.close()
				end
			end
		end,
		1,
		0,
	},
	["maxcalls"] = {
		function(c)
			if not tonumber(c) then error("Expected number for the maximum amount of calls, got " .. type(c), 0) end
			maxCalls = tonumber(c)
		end,
		1,
		1,
	},
	["maxtime"] = {
		function(c)
			if not tonumber(c) then error("Expected number for the maximum time, got " .. type(c), 0) end
			maxTime = tonumber(c)
		end,
		1,
		1,
	},
	["testonly"] = {
		function(list)
			if type(list) ~= "string" then error("Expected string as a list of tests, got " .. type(list), 0) end
			TaskList = {}
			for k, v in pairs(listToTable(list)) do
				TaskList[v] = true
			end
		end,
		1,
		1,
	},
	["exclude"] = {
		function(list)
			for k, v in pairs(listToTable(list)) do
				TaskList[v] = false
			end
		end,
		1,
		1,
	},
}
for i, v in pairs(args) do
	if v:sub(1, 2) == "--" then
		if not flags[v:sub(3, #v):lower()] then print("Hint: use '" .. shell.getRunningProgram() .. " help' to see the proper usage") error("Unknown flag: " .. v:sub(3, #v), 0) end
		flags[v:sub(3, #v):lower()]()

	elseif v:sub(1, 1) == "-" then
		if not properties[v:sub(2, #v):lower()] then print("Hint: use '" .. shell.getRunningProgram() .. " help' to see the proper usage") error("Unknown option: " .. v:sub(2, #v), 0) end
		local arg = {}

		for ii = i + 1, i + properties[v:sub(2, #v):lower()][2] do
			if not args[ii] then break end
			if args[ii]:sub(1, 1) == "-" then break end --if the argument starts with "-" then break end --and ii-i>properties[v:sub(2,#v):lower()][3] and minimum number of arguments has already been reached
			arg[#arg + 1] = args[ii]
			args[ii] = nil
		end

		properties[v:sub(2, #v):lower()][1](unpack(arg))

	elseif v:lower() == "help" or v == "?" or v == "/?" then
		if args[i + 1] == "light" then return help(true) end
		return help()

	elseif v:lower() == "update" then
		local u = fs.open("/.vBenchmarkUpdater.lua", "w")
		u.write("shell.run'rm " .. shell.getRunningProgram() .. "'shell.run'pastebin get nCEk57MH " .. shell.getRunningProgram() .. "'print'Update complete'sleep(3)return shell.run'" .. shell.getRunningProgram() .. " --stop'")
		u.close()
		return shell.run "/.vBenchmarkUpdater.lua"
	end
end

Test.SetTaskList(TaskList)

--------- BENCHMARK FUNCTIONS
local function display() --graphical operations (both term and graphics related calculations)
local w, h = term.getSize()
--- -text
-------- Optimized write
local function fill(c, bc, ch)
	term.setBackgroundColor(bc or colors.black)
	term.setTextColor(c or colors.white)
	term.clear()
	for y = 1, h do
		term.setCursorPos(1, y)
		term.write(string.rep(ch or "X", w))
	end
end

local ow = Test.New("ow", nil, maxCalls, maxTime, true, fill) --ow=optimized write, no group set (nil)->graphics, maxCalls max calls, maxTime maximum total benchmark time, true->yield, 'fill' function to benchmark
ow.Run()
print(unpack(unpack({ ow.GetResults() }))) --DEBUG

-------- Unoptimized write
local function fillUnoptimized(c, bc, ch)
	term.setBackgroundColor(bc or colors.black)
	term.setTextColor(c or colors.white)
	term.clear()
	for y = 1, h do
		for x = 1, w do
			term.setCursorPos(x, y)
			term.write(ch or "X")
		end
	end
end

local uw = Test.New("uw", nil, maxCalls, maxTime, true, fillUnoptimized)
uw.Run()

local function termBlitTest()
	for y = 1, h do
		term.setCursorPos(1, y)
		term.blit("asd")
	end
end

-------- Color test
local function testColor()
	for c = 0, 15 do
		term.setBackgroundColor(2 ^ c)
		term.clear()
	end
end

local ct = Test.New("ct", nil, maxCalls, maxTime, true, testColor)
ct.Run()
-------- Line clear
local function lineClear(c)
	term.setBackgroundColor(c or colors.black)
	for y = 1, h do
		term.setCursorPos(1, y)
		term.clearLine()
	end
end

local lc = Test.New("lc", nil, maxCalls, maxTime, true, lineClear)
lc.Run()

-------- Clear
term.setBackgroundColor(colors.black)
local clear = term.clear
local cs = Test.New("cs", nil, maxCalls, maxTime, true, clear) --TODO/WARN: supply just the term.clear() function?
cs.Run()

-------- Scroll
local function scroll()
	fill() --TODO any arguments?
	for y = 1, h do
		term.scroll(1)
	end
end

local scr = Test.New("scr", nil, maxCalls, maxTime, true, scroll)
scr.Run()

--- -2D graphics
------- Anti-Aliasing
local pixelWithAlpha = function(x, y, a) -- Pseudo Alpha channel. Only works with black background (for obvious reasons)

alphaStep = 255 / 4

if a > 255 - alphaStep then
	term.setBackgroundColor(colors.white)
elseif a > 255 - alphaStep * 2 then
	term.setBackgroundColor(colors.lightGray)
elseif a > 255 - alphaStep * 3 then
	term.setBackgroundColor(colors.gray)
else
	term.setBackgroundColor(colors.black)
	return
end

term.setCursorPos(x, y)
term.write(" ")
end
local aaLine = function(x0, y0, x1, y1)

	dx = math.abs(x1 - x0)
	if x0 < x1 then sx = 1 else sx = -1 end
	dy = math.abs(y1 - y0)
	if y0 < y1 then sy = 1 else sy = -1 end

	err = dx - dy

	if dx + dy == 0 then
		ed = 1
	else
		ed = math.sqrt(dx * dx + dy * dy)
	end

	while true do
		pixelWithAlpha(x0, y0, 255 - (255 * math.abs(err - dx + dy) / ed));
		e2 = err
		x2 = x0
		if (2 * e2 >= -dx) then
			if (x0 == x1) then break end
			if (e2 + dy < ed) then pixelWithAlpha(x0, y0 + sy, 255 - (255 * (e2 + dy) / ed)) end
			err = err - dy;
			x0 = x0 + sx;
		end
		if (2 * e2 <= dy) then
			if (y0 == y1) then break end
			if (dx - e2 < ed) then pixelWithAlpha(x2 + sx, y0, 255 - (255 * (dx - e2) / ed)) end
			err = err + dx;
			y0 = y0 + sy;
		end
	end
end

local w1, h1 = math.floor(w / 4), math.floor(h / 2)

local function aaTest()
	local minX, minY = 1, 1
	local maxX, maxY = 15, 15
	local x1, y1, x2, y2 = minX, minY, minX + 1, minY + 1
	return function()
		aaLine(x1, y1, x2, y2)
		if x2 > maxX then x2 = minX y2 = y2 + 1
		elseif y2 > maxY then --[[y2=minY y1=y1+1  end?]]
		elseif x1 > maxX then x1 = minX y1 = y1 + 1
		elseif y1 > maxY then y1 = minY x2 = x2 + 1
		else x1 = x1 + 1
		end
	end
end

local aa = Test.New("aa", nil, maxCalls, maxTime, true, aaTest())
aa.Run()
--aa.Start() --TODO/WARN ATTENTION IMPORTANT what about these? :?

--- -3D graphics (TODO)
end


local function mth() --mathematical operations
------- SHA256 Hashing
local l = 256
local str = ""
for i = 1, l do
	str = str ..
			string.char(math.random(128))
end
local sha = sha256.sha256
local hsh = Test.New("hsh", "Math", maxCalls, maxTime, true, sha, str)
hsh.Run()
------- CRC32 Hashing

local crch = crc32.Hash
--local crc=Test.New("crc","Math",maxCalls,maxTime,true,crch,str)
--crc.Run()

------- AES Encryption
local enc, ey, didFinish = false
local function aesEncrypt(k, l)
	local key = ""
	local str = ""
	for i = 1, k do
		key = key .. "b"
	end
	for i = 1, l do
		str = str .. string.char(math.random(128))
	end
	enc = aes.encrypt(key, str)
	ey = key
	didFinish = true
end

local aese = Test.New("aese", "Math", maxCalls, maxTime, true, aesEncrypt, 32, 2048)
aese.Run()
------- AES Decryption
local aesDecrypt = aes.decrypt
if didFinish then
	local aesd = Test.New("aesd", "Math", maxCalls, maxTime, true, aesDecrypt, ey, enc)
	aesd.Run()
end
------- Diffie-Hellman Key Exchange (by Anavrins)
local function dfhlKeyEx()
	local function modexp(remainder, exponent, modulo)
		for i = 1, exponent - 1 do
			remainder = remainder * remainder
			if remainder >= modulo then
				remainder = remainder % modulo
			end
		end
		return remainder
	end

	local public_base, public_primeMod = 11, 625210769
	local alice_secret = math.random(100000, 999999)
	local bob_secret = math.random(100000, 999999)

	local alice_public = modexp(public_base, alice_secret, public_primeMod)
	local bob_public = modexp(public_base, bob_secret, public_primeMod)

	modexp(bob_public, alice_secret, public_primeMod)
	modexp(alice_public, bob_secret, public_primeMod)
end

local dhke = Test.New("dhke", "Math", maxCalls, maxTime, true, dfhlKeyEx)
dhke.Run()
------- Compilation test 1
local function cmpl1()
	local code = [[
  do end
  ]]
	loadstring(code)
end

local cmpl1 = Test.New("cmpl1", "Math", maxCalls, maxTime, true, cmpl1)
cmpl1.Run()
------- Compilation test 2
local function cmpl2()
	local code = [[
  local function asd()
    while true do end
  end
  for i=1,10 do
    print(i)
  end
  return _G.x or asd
  ]]
	loadstring(code)
end

local cmpl2 = Test.New("cmpl2", "Math", maxCalls, maxTime, true, cmpl2)
cmpl2.Run()


------- Goniometry
local function gnm()
end
end

local function prph() --peripheral benchmark (whatever there is available is benchmarked :D)
end


local function flsys() --filesystem related tests
end


local function ascan() --scan for anomalies (like LOVE2D Lua mathematical bugs, bit lib etc)
end

local function memal() --memory allocation
local function newEntry()
	local t = {} --table for allocation entries
	for i = 1, 40000 do
		t[#t + 1] = true
	end
	t = nil
end

local tec = Test.New("tec", "Memory Allocation", maxCalls, maxTime, true, newEntry)
tec.Run()
local function delEntry()
	local t = {}
	for i = 1, 40000 do
		t[#t + 1] = true
	end
	for i, v in ipairs(t) do
		t[i] = nil
	end
	t = nil
end

local ted = Test.New("ted", "Memory Allocation", maxCalls, maxTime, true, delEntry)
ted.Run()
local function accEntry()
	local t = { true }
	local _ = false
	for i = 1, 40000 do
		_ = t[1]
	end
end

local tea = Test.New("tea", "Memory Allocation", maxCalls, maxTime, true, accEntry)
tea.Run()
end

----------------------
local wins, index = {}, 1 --wins=pages
wins[1] = window.create(term.native(), 1, 1, w, h - 5, true)
local function hideWins()
	for i, v in pairs(wins) do
		wins[i].setVisible(false)
	end
end

local function res() --display results
RESULT = Test.GetResults()
local last
if fs.exists(outputPath) then
	local f = fs.open(outputPath, "r")
	last = f.readAll()
	f.close()
	last = textutils.unserialize(last)
	if not last then
		local pth = outputPath .. "-unsupported.txt"
		local i = 1
		while fs.exists(pth) do
			pth = outputPath .. "-unsupported" .. i .. ".txt"
			i = i + 1
		end
		fs.move(outputPath, pth)
	end
	RESULT["Last"] = last
end

term.setBackgroundColor(colors.black)
term.setTextColor(colors.white)
term.clear()
term.setCursorPos(1, 1)
local groups = {}
for k, v in pairs(RESULT) do
	if k ~= "Last" and k ~= "Version" and k ~= "Median" then --median for future upgrade
	if not groups[v["g"]] then groups[v["g"]] = {} end
	table.insert(groups[v["g"]], k)
	end
end

for g, n in pairs(groups) do
	term.setTextColor(colors.yellow)
	print(g)
	--using pairs by KEYS not values => swap n so that keys are actual names and values are the original RESULT subtables
	local list = {}
	for k, v in pairs(n) do
		list[names[v]] = v
	end
	for name, v in pairsByKeys(list) do
		if TaskList[v] then
			term.setTextColor(colors.white)
			term.write("  " .. name .. ": ")
			term.setTextColor(colors.lightGray)
			term.write((RESULT[v]["mark"]) .. "")

			if last and last[v] and type(last[v]["mark"]) == "number" and (last.Version and (last.Version.Library and (Test.Version and (last.Version.Library.Matrix == Test.Version.Matrix)))) then -- safe check (no nil access attempts hopefully) for compatible Matrix version
			term.setTextColor((last[v]["mark"] > RESULT[v]["mark"] and colors.red or colors.lime))
			term.write(" " .. (last[v]["mark"] > RESULT[v]["mark"] and "v" or "^"))
			term.setTextColor(colors.lightGray)
			print(" (" .. (last[v]["mark"]) .. ")")
			else
				print()
			end
			local _x, _y = term.getCursorPos()
			if _y > h - 9 then --10 for safe end (no scrolling)
			wins[#wins + 1] = window.create(term.native(), 1, 1, w, h - 5, false)
			term.redirect(wins[#wins])
			end
		end
	end
end
term.redirect(term.native())
end

local function render()
	hideWins()
	wins[index].setVisible(true)
	wins[index].redraw()
	local _x, _y = term.getCursorPos()

	term.setBackgroundColor(colors.black)
	term.setTextColor(colors.gray)
	term.setCursorPos(w - 10, h - 5)

	term.write("page ")
	term.setTextColor(colors.lightGray)
	term.write(index .. "/" .. #wins .. "    ") --spaces to clean if #wins>10
	term.setCursorPos(_x, _y)
end

--- -MAIN CODE
local function run()
	display()
	mth()
	prph()
	flsys()
	ascan()
	memal()
	--TODO: CC API benchmark (parallel, textutils, etc)
end


run()
term.setBackgroundColor(colors.black)
term.clear()

term.redirect(wins[1])
res()
term.setCursorPos(1, h - 5)

local _x, _y = term.getCursorPos()
term.setCursorPos(w / 2 - 5, _y + 2)
term.setTextColor(colors.gray)
term.write "Loading..."
term.setTextColor(colors.white)
term.setCursorPos(_x, _y)

local selfie = fs.open(shell.getRunningProgram(), "r")
local ip = "''"

if http then
	local h = http.get "http://acos.bluefile.cz/Utilities/Online/ip.php"
	if h then
		ip = h.readAll()
		h.close()
	end
end

ip = textutils.unserialize(ip)

local c = selfie.readAll()
selfie.close()
Version.Hash = sha256.sha256(c)

Version.Fingerprint = sha256.sha256(Version.Hash .. "Fingerprint" .. os.getComputerID() .. ip)
RESULT["Version"] = Version

local save = fs.open(outputPath, "w")
save.write("--viluon's Ultimate Benchmark output, os.time()=" .. os.time() .. ", os.day()=" .. os.day() .. "\n\n")
save.write(textutils.serialize(RESULT))
s ""
save.close()
local r, l = 0, RESULT.Last
while true do
	r = r + 1
	if l == nil then break end
	l = (l and l.Last or nil) --first run fix
end

print ""
print("Total runs:" .. r)
print("Results were logged (see " .. outputPath .. "). Would you like to re-run the benchmark now?")
print ""
term.setBackgroundColor(colors.green)
term.setTextColor(colors.white)
term.write(string.rep(" ", ((w / 2) / 2) - 4) .. "[Re-run]" .. string.rep(" ", ((w / 2) / 2) - 3))
term.setBackgroundColor(colors.red)
term.setTextColor(colors.black)
term.write(string.rep(" ", ((w / 2) / 2) - 3) .. "[Exit]" .. string.rep(" ", ((w / 2) / 2) - 1))

local rerun = false

while true do
	render()
	local ev = { coroutine.yield() }
	if ev[1] == "mouse_scroll" then
		index = index + ev[2]
	elseif ev[1] == "mouse_click" and ev[4] == h then
		if ev[3] < w / 2 then
			rerun = true
			break
		else
			local lines = {
				"       ______________________________________",
				"      / | \\            |                 |  ",
				"_    /  |_/  _       _ |_         _      |  ",
				" \\  /   | \\ /_\\ +-+ /  | | +-+-+ '_\\ ,_  |/ ",
				"  \\/    |_/ \\_  | | \\_ | | | | | (_| | ' |\\ "
			}

			local w, h = term.getSize()
			local X = w / 2
			local win = window.create(term.native(), X, 1, #lines[1], 5, true)

			term.setBackgroundColor(colors.black)
			term.clear()
			term.setCursorPos(2, 6)
			term.write "(c) "
			term.setTextColor(colors.yellow)
			term.write "viluon"
			term.setTextColor(colors.white)
			term.write " 2015"


			term.redirect(win)

			term.setTextColor(colors.yellow)


			for i, v in ipairs(lines) do
				term.setCursorPos(1, i)
				term.write(v:sub(1, 6))
			end

			term.setTextColor(colors.white)
			term.setCursorPos(1, 3)
			term.write "_"

			for i = 6, #lines[1] do
				for k, v in ipairs(lines) do
					term.setCursorPos(i, k)
					term.write(v:sub(i, i))
				end
				X = X - ((w / 2 - 2) / (#lines[1] - 6))
				win.reposition(X, 1)
				sleep(0)
			end
			term.redirect(term.native())
			term.setCursorPos(w - #Version.App - (w - #lines[1]), 6)
			term.write("v" .. Version.App)

			sleep(1)
			break
		end
	elseif ev[1] == "key" then
		if ev[2] == keys.right or ev[2] == keys.pageDown then
			index = index + 1
		elseif ev[2] == keys.left or ev[2] == keys.pageUp then
			index = index - 1
		end
	end
	if index < 1 then
		index = 1
	elseif index > #wins then
		index = #wins
	end
end

if rerun then
	term.setTextColor(colors.white) shell.run "clear" return shell.run(shell.getRunningProgram(), unpack(Args))
end
