local function e(j)
	local x = setmetatable({}, { __index = _ENV or getfenv() }) if setfenv then setfenv(j, x) end; return j(x) or x
end

local t = e(function(j, ...) local x, z = os.queueEvent, coroutine.yield; local function _() x("sleep") if z() == "terminate" then
	error("Terminated")
end
end

; return
{ dir = shell.dir, refreshYield = _, serialize = textutils.serialize }
end)
local a = e(function(j, ...) local function x(E)
	if type(E) == "string" then return string.format("%q", E) else return tostring(E) end
end

local function z(E, T, A, O) local I = "" local N = type(E)
if
N == "table" then local S = A[E]
if S then I = I .. (T .. "--[[ Object@" .. S .. " ]] { }") ..
	"\n" else
	S = A.length + 1; A[E] = S; A.length = S; I = I ..
		(T .. "--[[ Object@" .. S .. " ]] {") .. "\n"
	for H, R in pairs(E) do
		if
		type(H) == "table" then I = I .. (T .. "\t{") .. "\n" I = I ..
			z(H, T .. "\t\t", A, O) I = I .. z(R, T .. "\t\t", A, O) I = I ..
			(T .. "\t},") .. "\n" elseif type(R) == "table" then
			I = I .. (T ..
				"\t[" .. x(H) .. "] = {") .. "\n" I = I .. z(R, T .. "\t\t", A, O)
			I = I .. (T .. "\t},") .. "\n" else I = I ..
			(T .. "\t[" .. x(H) .. "] = " .. x(R) .. ",") .. "\n"
		end
	end
	if O then local H = getmetatable(E) if H then
		I = I .. (T .. "\tMetatable = {") .. "\n" I = I .. z(H, T .. "\t\t", A, O)
		I = I .. (T .. "\t}") .. "\n"
	end
	end; I = I .. (T .. "}") .. "\n"
end else I = I .. (T .. x(E)) .. "\n"
end; return I
end

local function _(E, T, A) if T == nil then T = true end; return z(E, A or "", { length = 0 }, T) end

; return _
end)
local o = e(function(j, ...) local x = false; local function z(C, ...) local M = term.isColor()
if M then term.setTextColor(C) end; print(...)
if M then term.setTextColor(colors.white) end
end

local function _(C, M)
	local F = term.isColor() if F then term.setTextColor(C) end; io.write(M) if F then
		term.setTextColor(colors.white)
	end
end

; local function E(...) z(colors.green, ...) end

; local function T(...)
	z(colors.red, ...)
end

; local function A(C) if C ~= nil then x = C end; return x end

local function O(...) if x then local C, M = pcall(function()
	error("", 4)
end) _(colors.gray, M)
z(colors.lightGray, ...)
end
end

local function I(...)
	if x then local C, M = pcall(function() error("", 4) end)
	_(colors.gray, M) local F = false
	for W, Y in ipairs({ ... }) do local P = type(Y) if P == "table" then local V = a or t.serialize
	Y = V(Y) else Y = tostring(Y)
	end; if F then Y = " " .. Y end
	F = true; _(colors.lightGray, Y)
	end; print()
	end
end

local N = { ["^"] = "%^", ["$"] = "%$", ["("] = "%(", [")"] = "%)", ["%"] = "%%", ["."] = "%.", ["["] = "%[", ["]"] = "%]", ["*"] = "%*", ["+"] = "%+", ["-"] = "%-", ["?"] = "%?", ["\0"] = "%z" } local function S(C) return (C:gsub(".", N)) end

local H = { ["^"] = "%^", ["$"] = "%$", ["("] = "%(", [")"] = "%)", ["%"] = "%%", ["."] = "%.", ["["] = "%[", ["]"] = "%]", ["+"] = "%+", ["-"] = "%-", ["?"] = "%?", ["\0"] = "%z" }
local function R(C, M) local F = C:sub(1, 5)
if F == "ptrn:" or F == "wild:" then local C = C:sub(6)
if F == "wild:" then
	if M then
		local W = 0
		C = ((C:gsub(".", H)):gsub("(%*)", function() W = W + 1; return "%" .. W end)) else C = "^" ..
		((C:gsub(".", H)):gsub("(%*)", "(.*)")) .. "$"
	end
end; return { Type = "Pattern", Text = C } else return { Type = "Normal", Text = C }
end
end

; local function D(C) for M, F in ipairs(C) do C[F] = true end; return C end

; local function L(C, M)
	local F = #C; if F ~= #M then return false end
	for W = 1, F do if C[W] ~= M[W] then return false end end; return true
end

local U = print
return
{ Print = U, PrintError = T, PrintSuccess = E, PrintColor = z, WriteColor = _, IsVerbose = A, Verbose = O, VerboseLog = I, EscapePattern = S, ParsePattern = R, CreateLookup = D, MatchTables = L }
end)
local i = e(function(j, ...) local x, z = type, pairs; local _ = {}
function _:update(D) if D then self.fn = D.fn or self.fn; self.options = D.options or
	self.options
end
end

local function E(D, L) return
setmetatable({ options = L or {}, fn = D, channel = nil, id = math.random(1000000000) }, { __index = _ })
end

; local T = {}
local function A(D, L) return
setmetatable({ stopped = false, namespace = D, callbacks = {}, channels = {}, parent = L }, { __index = T })
end

function T:addSubscriber(D, L) local U = E(D, L) local C = (#self.callbacks + 1)
L = L or {} if L.priority and L.priority >= 0 and L.priority < C then
	C = L.priority
end
table.insert(self.callbacks, C, U) return U
end

function T:getSubscriber(D) for U = 1, #self.callbacks do local C = self.callbacks[U] if C.id == D then
	return { index = U, value = C }
end
end; local L
for U, C in
z(self.channels) do L = C:getSubscriber(D) if L then break end
end; return L
end

function T:setPriority(D, L) local U = self:getSubscriber(D)
if U.value then
	table.remove(self.callbacks, U.index) table.insert(self.callbacks, L, U.value)
end
end

function T:addChannel(D) self.channels[D] = A(D, self) return self.channels[D] end

function T:hasChannel(D) return D and self.channels[D] and true end

; function T:getChannel(D)
	return self.channels[D] or self:addChannel(D)
end

function T:removeSubscriber(D)
	local L = self:getSubscriber(D)
	if L and L.value then
		for U, C in z(self.channels) do C:removeSubscriber(D) end; return table.remove(self.callbacks, L.index)
	end
end

function T:publish(D, ...)
	for L = 1, #self.callbacks do local U = self.callbacks[L]
	if
	not U.options.predicate or U.options.predicate(...) then local C, M = U.fn(...) if M then
		D[#D] = M
	end; if C == false then return false, D end
	end
	end
	if parent then return parent:publish(D, ...) else return true, D end
end

; local O = A('root')
local function I(D) local O = O
if x(D) == "string" then if D:find(":") then
	D = { D:match((D:gsub("[^:]+:?", "([^:]+):?"))) } else D = { D }
end
end; for L = 1, #D do O = O:getChannel(D[L]) end; return O
end

; local function N(D, L, U) return I(D):addSubscriber(L, U) end

; local function S(D, L) return
I(L):getSubscriber(D)
end

; local function H(D, L)
	return I(L):removeSubscriber(D)
end

local function R(D, ...) return I(D):publish({}, ...) end

return { GetChannel = I, Subscribe = N, GetSubscriber = S, RemoveSubscriber = H, Publish = R }
end)
local n = e(function(j, ...)
	local x = {
		__index = function(E, T) return
		function(A, ...) local O = A.parser; local I = O[T](O, A.name, ...) if I == O then return A end; return I end
		end
	} local z = {}
	function z:Get(E, T) local A = self.options; local O = A[E] if O ~= nil then return O end
	local I = self.settings[E]
	if I then local N = I.aliases; if N then
		for S, H in ipairs(N) do O = A[H] if O ~= nil then return O end end
	end; O = I.default; if O ~= nil then return O end
	end; return T
	end

	; function z:Ensure(E) local T = self:Get(E)
	if T == nil then error(E .. " must be set") end; return T
	end

	; function z:Default(E, T)
		if T == nil then T = true end; self:_SetSetting(E, "default", T) self:_Changed()
		return self
	end

	function z:Alias(E, T)
		local A = self.settings; local O = A[E] if O then local I = O.aliases
		if I == nil then O.aliases = { T } else table.insert(I, T) end else A[E] = { aliases = { T } }
		end
		self:_Changed() return self
	end

	function z:Description(E, T) return self:_SetSetting(E, "description", T) end

	; function z:TakesValue(E, T) if T == nil then T = true end
	return self:_SetSetting(E, "takesValue", T)
	end

	function z:_SetSetting(E, T, A) local O = self.settings
	local I = O[E] if I then I[T] = A else O[E] = { [T] = A } end; return self
	end

	function z:Option(E) return setmetatable({ name = E, parser = self }, x) end

	; function z:Arguments() return self.arguments end

	; function z:_Changed()
		i.Publish({ "ArgParse", "changed" }, self)
	end

	function z:Help(E)
		for T, A in pairs(self.settings) do local O = '-' if A.takesValue then O = "--" T =
		T .. "=value"
		end; if #T > 1 then O = '--' end; o.WriteColor(colors.white, E ..
			O .. T) local I = "" local N = A.aliases
		if N and #N > 0 then
			local H = #N; I = I .. " (" for R = 1, H do local D = "-" .. N[R] if #D > 2 then D = "-" .. D end
			if R < H then D = D .. ', ' end; I = I .. D
			end; I = I .. ")"
		end; o.WriteColor(colors.brown, I) local S = A.description; if S and S ~= "" then o.PrintColor(colors.lightGray,
			" " .. S)
		end
		end
	end

	function z:Parse(E) local T = self.options; local A = self.arguments
	for O, I in ipairs(E) do
		if I:sub(1, 1) == "-" then
			if
			I:sub(2, 2) == "-" then local N, S = I:match("([%w_%-]+)=([%w_%-]+)", 3) if N then T[N] = S else
				I = I:sub(3) local H = I:sub(1, 4) local S = true
				if H == "not-" or H == "not_" then S = false; I = I:sub(5) end; T[I] = S
			end else for N = 2, #I do
				T[I:sub(N, N)] = true
			end
			end else table.insert(A, I)
		end
	end; return self
	end

	local function _(E) return
	setmetatable({ options = {}, arguments = {}, settings = {} }, { __index = z }):Parse(E)
	end

	; return { Parser = z, Options = _ }
end)
local s = e(function(j, ...)
	local function x() local E = t.dir() local T = { "Howlfile", "Howlfile.lua" }
	while true do
		for A, O in ipairs(T) do
			howlFile = fs.combine(E, O) if fs.exists(howlFile) and not fs.isDir(howlFile) then
				return O, E
			end
		end; if E == "/" or E == "" then break end; E = fs.getDir(E)
	end; return nil,
	"Cannot find HowlFile. Looking for '" .. table.concat(T, "', '") .. "'"
	end

	; local z = {}
	local function _(E)
		local T = setmetatable(E or {}, { __index = getfenv() }) T._G = _G
		function T.loadfile(A) return setfenv(loadfile(A), T) end

		; function T.dofile(A) return T.loadfile(A)() end

		i.Publish({ "HowlFile", "env" }, T) return T
	end

	; return { FindHowl = x, SetupEnvironment = _, CurrentDirectory = "" }
end)
local h = e(function(j, ...) local x = {}
function x:DoRequire(E, T) if self.filesProduced[E] then return true end
local A = self.producesCache[E]
if A then self.filesProduced[E] = true; return self:Run(A) end; A = self.normalMapsCache[E] local O, I; local N = E; if A then
	self.filesProduced[E] = true; I = A.Name; O = A.Pattern.From
end
for S, H in
pairs(self.patternMapsCache) do if E:match(S) then self.filesProduced[E] = true; I = H.Name
O = E:gsub(S, H.Pattern.From) break
end
end
if I then local S = self:DoRequire(O, true)
if not S then if not T then
	o.PrintError("Cannot find '" .. O .. "'")
end; return false
end; return self:Run(I, O, N)
end; if fs.exists(fs.combine(s.CurrentDirectory, E)) then
	self.filesProduced[E] = true; return true
end; if not T then
	o.PrintError("Cannot find a task matching '" .. E .. "'")
end; return false
end

; local function z(E, T) local A = #E; if #E ~= #T then return false end
for O = 1, A do if E[O] ~= T[O] then return false end end; return true
end

function x:Run(E, ...)
	local T = E
	if type(E) == "string" then T = self.tasks[E] if not T then
		o.PrintError("Cannot find a task called '" .. E .. "'") return false
	end elseif not T or not T.Run then
		o.PrintError("Cannot call task as it has no 'Run' method") return false
	end; local A = { ... } local O = self.ran[T]
	if not O then O = { A } self.ran[T] = O else for I = 1, #O do if z(A, O[I]) then
		return true
	end
	end; O[#O + 1] = A
	end; t.refreshYield() return T:Run(self, ...)
end

function x:Start(E) local T
if E then T = self.tasks[E] else T = self.default; E = "<default>" end; if not T then
	o.PrintError("Cannot find a task called '" .. E .. "'") return false
end; return self:Run(T)
end

function x:BuildCache() local E = {} local T = {} local A = {} self.producesCache = E; self.patternMapsCache = T
self.normalMapsCache = A
for O, I in pairs(self.tasks) do local N = I.produces; if N then
	for H, R in ipairs(N) do local D = E[R] if D then
		error(string.format("Both '%s' and '%s' produces '%s'", D, O, R))
	end; E[R] = O
	end
end
local S = I.maps
if S then
	for H, R in ipairs(S) do
		local D = (R.Type == "Pattern" and T or A) local L = R.To; local U = D[L] if U then
			error(string.format("Both '%s' and '%s' match '%s'", U, O, L))
		end; D[L] = { Name = O, Pattern = R }
	end
end
end; return self
end

local function _(E) return
setmetatable({ ran = {}, filesProduced = {}, tasks = E.tasks, default = E.default, Traceback = E.Traceback, ShowTime = E.ShowTime }, { __index = x }):BuildCache()
end

; return { Factory = _, Context = x }
end)
local r = e(function(j, ...)
	local function x(T, A) local O = o.ParsePattern(T, true) local I = o.ParsePattern(A)
	local N = O.Type
	assert(N == I.Type, "Both from and to must be the same type " .. N .. " and " .. O.Type) return { Type = N, From = O.Text, To = I.Text }
	end

	; local z = {}
	function z:Depends(T)
		if type(T) == "table" then local A = self.dependencies; for O, I in ipairs(T) do
			table.insert(A, I)
		end else table.insert(self.dependencies, T)
		end; return self
	end

	function z:Requires(T)
		if type(T) == "table" then local A = self.requires; for O, T in ipairs(T) do
			table.insert(A, T)
		end else table.insert(self.requires, T)
		end; return self
	end

	function z:Produces(T)
		if type(T) == "table" then local A = self.produces; for O, T in ipairs(T) do
			table.insert(A, T)
		end else table.insert(self.produces, T)
		end; return self
	end

	function z:Maps(T, A) table.insert(self.maps, x(T, A)) return self end

	; function z:Action(T) self.action = T; return self end

	; function z:Description(T)
		self.description = T; return self
	end

	; function z:_RunAction(...)
		return self.action(self, ...)
	end

	function z:Run(T, ...) for A, O in ipairs(self.dependencies) do if not T:Run(O) then
		return false
	end
	end
	for A, O in
	ipairs(self.requires) do if not T:DoRequire(O) then return false end
	end
	for A, O in ipairs(self.produces) do T.filesProduced[O] = true end
	if self.action then local A = { ... } local O = ""
	if #A > 0 then newArgs = {} for H, R in ipairs(A) do
		table.insert(newArgs, tostring(R))
	end; O = " (" ..
		table.concat(newArgs, ", ") .. ")"
	end
	o.PrintColor(colors.cyan, "Running " .. self.name .. O) local I = os.clock() local N, S = true, nil
	if T.Traceback then
		xpcall(function()
			self:_RunAction(unpack(A))
		end, function(H)
			for R = 5, 15 do
				local D, S = pcall(function() error("", R) end) if H:match("Howlfile") then break end; H = H .. "\n  " .. S
			end; S = H; N = false
		end) else N, S = pcall(self._RunAction, self, ...)
	end
	if N then o.PrintSuccess(self.name .. ": Success") else o.PrintError(self.name .. ": Failure\n" .. S) end; if T.ShowTime then
		o.Print(" ", "Took " .. os.clock() - I .. "s")
	end; return N
	end; return true
	end

	local _ = {
		__index = function(T, A) local O = z[A] if O then return O end
		if A:match("^%u") then local I = z[A] if I then return I end
		return function(T, N)
			if N == nil then N = true end; T[(A:gsub("^%u", string.lower))] = N; return
			T
		end
		end
		end
	}
	local function E(T, A, O, I) if type(A) == "function" then O = A; A = {} end
	return setmetatable({ name = T, action = O, dependencies = A or {}, description = nil, maps = {}, requires = {}, produces = {} },
		I or { __index = z })
	end

	; return { Factory = E, Task = z, OptionTask = _ }
end)
local d = e(function(j, ...) local x = {} function x:Task(_)
	return function(E, T) return self:AddTask(_, E, T) end
end

; function x:AddTask(_, E, T) return
self:InjectTask(r.Factory(_, E, T))
end

; function x:InjectTask(_, E)
	self.tasks[E or _.name] = _; return _
end

function x:Default(_) local E
if _ == nil then self.default = nil elseif type(_) ==
	"string" then self.default = self.tasks[_] if not self.default then
	error("Cannot find task " .. _)
end else
	self.default = r.Factory("<default>", {}, _)
end; return self
end

; function x:Run(_) return self:RunMany({ _ }) end

function x:RunMany(_)
	local E = os.clock() local T; local A = h.Factory(self) if #_ == 0 then A:Start() else
		for O, I in ipairs(_) do T = A:Start(I) end
	end; if A.ShowTime then
		o.PrintColor(colors.orange, "Took " ..
			os.clock() - E .. "s in total")
	end; return T
end

; local function z()
	return setmetatable({ tasks = {}, default = nil }, { __index = x })
end

; return { Factory = z, Runner = x }
end)
local l = e(function(j, ...) local x = {}
function x:Name(E) self.name = E; self:Alias(E) return self end

; function x:Alias(E) self.alias = E; return self end

function x:Depends(E) if type(E) == "table" then for T, A in
ipairs(E) do self:Depends(A)
end else
	table.insert(self.dependencies, E)
end; return self
end

function x:Prerequisite(E)
	if type(E) == "table" then
		for T, A in ipairs(E) do self:Prerequisite(A) end else table.insert(self.dependencies, 1, E)
	end; return self
end

function x:Export(E) if E == nil then E = true end; self.shouldExport = E; return self end

function x:NoWrap(E) if E == nil then E = true end; self.noWrap = E; return self end

; local z = {}
local function _(E, T) return
setmetatable({ mainFiles = {}, files = {}, path = E or s.CurrentDirectory, namespaces = {}, shouldExport = false, parent = T }, { __index = z })
end

; function z:File(E) local T = self:_File(E) self.files[E] = T
i.Publish({ "Dependencies", "create" }, self, T) return T
end

; function z:Resource(E)
	local T = self:_File(E) T.type = "Resource" self.files[E] = T
	i.Publish({ "Dependencies", "create" }, self, T) return T
end

function z:_File(E) return
setmetatable({
	dependencies = {},
	name =
	nil,
	alias = nil,
	path = E,
	shouldExport = true,
	noWrap = false,
	type = "File",
	parent = self
}, { __index = x })
end

function z:Main(E) local T = self:FindFile(E) or self:_File(E)
T.type = "Main" table.insert(self.mainFiles, T)
i.Publish({ "Dependencies", "create" }, self, T) return T
end

function z:Depends(E) local T = self.mainFiles[1]
assert(T, "Cannot find a main file") T:Depends(E) return self
end

function z:Prerequisite(E) local T = self.mainFiles[1]
assert(T, "Cannot find a main file") T:Prerequisite(E) return self
end

function z:FindFile(E) local T = self.files; local A = T[E] if A then return A end; A = T[E .. ".lua"]
if A then return A end; for O, A in pairs(T) do if A.alias == E then return A end end; return nil
end

function z:Iterate() local E = {}
local function T(O) if E[O.path] then return end; E[O.path] = true; for I, N in ipairs(O.dependencies) do
	local S = self:FindFile(N)
	if not S then error("Cannot find file " .. N) end; T(S)
end
coroutine.yield(O)
end

; local A = self.mainFiles; if #A == 0 then A = self.files end
return coroutine.wrap(function() for O, I in pairs(A) do
	T(I)
end
end)
end

function z:Export(E) if E == nil then E = true end; self.shouldExport = E; return self end

function z:Namespace(E, T, A)
	local O = _(fs.combine(self.path, T or ""), self) self.namespaces[E] = O; A(O) return O
end

function z:CloneDependencies(E) local T = setmetatable({}, { __index = z })
for A, O in pairs(self) do T[A] = O end; T.mainFiles = {} return T
end

; function z:Paths() local E, T = table.insert, {}
for A, O in pairs(self.files) do E(T, O.path) end; return T
end

i.Subscribe({ "HowlFile", "env" }, function(E)
	E.Dependencies = _; E.Sources = _()
end) return { File = x, Dependencies = z, Factory = _ }
end)
do local j = string.format
local x = [[
local args = {...}
xpcall(function()
	(function(...)
]]
local z = [[
	end)(unpack(args))
end, function(err)
	printError(err)
	for i = 3, 15 do
		local s, msg = pcall(error, "", i)
		if msg:match("xpcall") then break end
		printError("  ", msg)
	end
	error(err:match(":.+"):sub(2), 3)
end)
]]
local _ = [[
local env = setmetatable({}, {__index = getfenv()})
local function openFile(filePath)
	local f = assert(fs.open(filePath, "r"), "Cannot open " .. filePath)
	local contents = f.readAll()
	f.close()
	return contents
end
local function doWithResult(file)
	local currentEnv = setmetatable({}, {__index = env})
	local result = setfenv(assert(loadfile(file), "Cannot find " .. file), currentEnv)()
	if result ~= nil then return result end
	return currentEnv
end
local function doFile(file, ...)
	return setfenv(assert(loadfile(file), "Cannot find " .. file), env)(...)
end
]]
function l.Dependencies:CreateBootstrap(E, T) local A = self.path
local O = fs.open(fs.combine(s.CurrentDirectory, E), "w") assert(O, "Could not create" .. E) if T.traceback then
	O.writeLine(x)
end; O.writeLine(_) local I = {}
for N in self:Iterate() do
	local S = j("%q", fs.combine(A, N.path)) local H = N.name
	if N.type == "Main" then
		O.writeLine("doFile(" .. S .. ", ...)") elseif N.type == "Resource" then
		O.writeLine("env[" .. j("%q", H) "] = openFile(" .. S .. ")") elseif H then
		O.writeLine("env[" ..
			j("%q", H) .. "] = " ..
			(N.noWrap and "doFile" or "doWithResult") .. "(" .. S .. ")") else O.writeLine("doFile(" .. S .. ")")
	end
end; if T.traceback then O.writeLine(z) end; O.close()
end

function d.Runner:CreateBootstrap(E, T, A, O)
	return
	self:InjectTask(r.Factory(E, O, function(I) T:CreateBootstrap(A, I) end, r.OptionTask)):Description("Creates a 'dynamic' combination of files in '" .. A .. "')"):Produces(A):Requires(T:Paths())
end
end
do local j = loadstring
i.Subscribe({ "Combiner", "include" }, function(x, z, _, E)
	if E.verify and z.verify ~= false then
		local T, A = j(_)
		if not T then local O = z.path; local I = "Could not load " ..
			(O and ("file " .. O) or "string") if A ~= "nil" then
			I = I .. ":\n" .. A
		end; return false, I
		end
	end
end)
i.Subscribe({ "Dependencies", "create" }, function(x, z)
	if z.type == "Resource" then z:Verify(false) end
end)
function l.File:Verify(x) if x == nil then x = true end; self.verify = x; return self end
end
do local j = string.find
local x = {
	header = [[
		-- Maps
		local lineToModule = setmetatable({{lineToModule}}, {
			__index = function(t, k)
				if k > 1 then return t[k-1] end
			end
		})
		local moduleStarts = {{moduleStarts}}
		local programEnd = {{lastLine}}

		-- Stores the current file, safer than shell.getRunningProgram()
		local _, currentFile = pcall(error, "", 2)
		currentFile = currentFile:match("[^:]+")
	]],
	updateError = [[
		-- If we are in the current file then we should map to the old modules
		if filename == currentFile then

			-- If this line is after the program end then
			-- something is broken, and so we just roll with it
			if line > programEnd then return end

			-- convert to module lines
			filename = lineToModule[line] or "<?>"
			local newLine = moduleStarts[filename]
			if newLine then
				line = line - newLine + 1
			else
				line = -1
			end
		end
	]]
}
local z = {
	header = [[
		local finalizer = function(message, traceback) {{finalizer}} end
	]],
	parseTrace = [[
		local ok, finaliserError = pcall(finalizer, message, traceback)

		if not ok then
			printError("Finalizer Error: ", finaliserError)
		end
	]]
}
local _ = ([[
end
-- The main program executor
	local args = {...}
	local currentTerm = term.current()
	local ok, returns = xpcall(
		function() return {__program(unpack(args))} end,
		function(message)
			local _, err = pcall(function()
			local error, pcall, printError, tostring,setmetatable = error, pcall, printError, tostring, setmetatable
			{{header}}

			local messageMeta = {
				__tostring = function(self)
					local msg = self[1] or "<?>"
					if self[2] then msg = msg .. ":" .. tostring(self[2]) end
					if self[3] and self[3] ~= " " then msg = msg .. ":" .. tostring(self[3]) end
					return msg
				end
			}
			local function updateError(err)
				local filename, line, message = err:match("([^:]+):(%d+):?(.*)")
				-- Something is really broken if we can't find a filename
				-- If we can't find a line number than we must have `pcall:` or `xpcall`
				-- This means, we shouldn't have an error, so we must be debugging somewhere
				if not filename or not line then return end
				line = tonumber(line)
				{{updateError}}
				return setmetatable({filename, line, message}, messageMeta)
			end

			-- Reset terminal
			term.redirect(currentTerm)

			-- Build a traceback
			local topError = updateError(message) or message
			local traceback = {topError}
			for i = 6, 6 + 18 do
				local _, err = pcall(error, "", i)
				err = updateError(err)
				if not err then break end
				traceback[#traceback + 1] = err
			end

			{{parseTrace}}

			printError(tostring(topError))
			if #traceback > 1 then
				printError("Raw Stack Trace:")
				for i = 2, #traceback do
					printError("  ", tostring(traceback[i]))
				end
			end
			end)
			if not _ then printError(err) end
		end
	)

	if ok then
		return unpack(returns)
	end
]])
local function E(O) local I, N, S = 1, 1, 1; local H = 1; local R = #O; while I < R do N, S = j(O, '\n', I, true) if not N then break end; H = H + 1
I = S + 1
end; return H
end

; local function T(O, I)
	return O:gsub("{{(.-)}}", function(N) return I[N] or "" end)
end

i.Subscribe({ "Combiner", "start" }, function(O, I, N) if O.finalizer then
	N.traceback = true
end; if N.lineMapping then N.oldLine = 0; N.line = 0; N.lineToModule = {}
N.moduleStarts = {}
end; if N.traceback then
	I.write("local __program = function(...)")
end
end) local A = math.min
i.Subscribe({ "Combiner", "write" }, function(O, I, N, S)
	if S.lineMapping then I = I or "file" local H = S.line
	S.oldLine = H; local R = H + E(N) S.line = R; H = H + 1; R = R - 1; local D, L = S.moduleStarts, S.lineToModule
	local U = D[I] if U then D[I] = A(H, U) else D[I] = H end; L[A(H, R)] = I
	end
end)
i.Subscribe({ "Combiner", "end" }, function(O, I, N)
	if N.traceback then local S = {} local H = {}
	if O.finalizer then local R = O.finalizer.path
	local D = fs.combine(O.path, R)
	local L = assert(fs.open(D, "r"), "Finalizer " .. D .. " does not exist") finalizerContents = L.readAll() L.close() if #finalizerContents == 0 then finalizerContents =
	nil else
		i.Publish({ "Combiner", "include" }, O, z, finalizerContents, N)
	end; if finalizerContents then
		S[#S + 1] = z; H.finalizer = finalizerContents
	end
	end
	if N.lineMapping then S[#S + 1] = x; local R = t.serialize
	H.lineToModule = R(N.lineToModule) H.moduleStarts = R(N.moduleStarts) H.lastLine = N.line
	end; toReplace = {}
	for R, D in ipairs(S) do for L, U in pairs(D) do local C = toReplace[L]
	if C then C = C .. "\n" else C = "" end; toReplace[L] = C .. U
	end
	end; I.write(T(T(_, toReplace), H))
	end
end)
function l.Dependencies:Finalizer(O)
	local I = self:FindFile(O) or self:File(O) I.type = "Finalizer" self.finalizer = I
	i.Publish({ "Dependencies", "create" }, self, I) return I
end
end
do local j = i.GetChannel { "Combiner" } local x = "_W"
local z = ("local function " .. x ..
	[[(f)
		local e=setmetatable({}, {__index = _ENV or getfenv()})
		if setfenv then setfenv(f, e) end
		return f(e) or e
	end]]):gsub("[\t\n ]+", " ")
function l.Dependencies:Combiner(_, E) E = E or {} local T = self.path; local A = self.shouldExport
local O, I = loadstring, t.serialize
local N = fs.open(fs.combine(s.CurrentDirectory, _), "w") assert(N, "Could not create " .. _)
local S = j:getChannel("include") local H, R; do local L = N.writeLine; local U = j:getChannel("write") local C = U.publish; R = function(M, F) if
C(U, {}, self, F, M, E) then L(M)
end
end
H = { write = R, path = _ }
end
j:getChannel("start"):publish({}, self, H, E) if E.header ~= false then R(z) end; local D = {}
for L in self:Iterate() do local U = L.path
local C = fs.open(fs.combine(T, U), "r")
assert(C, "File " .. U .. " does not exist") local M = C.readAll() C.close() local F, W = S:publish({}, self, L, M, E)
if not F then
	N.close() error(W[#W] or "Unknown error")
end; o.Verbose("Adding " .. U) local Y = L.name
if L.type == "Main" then
	R(M, L.alias or L.path) elseif L.type == "Resource" then local P =
assert(Y, "A name must be specified for resource " .. L.path) .. "="
if not
L.shouldExport then P = "local " .. P elseif not A then D[#D + 1] = Y; P = "local " .. P
end; R(P .. I(M), L.alias or L.path) elseif Y then
	local P, V = x .. '(function(_ENV, ...)', 'end)' if L.noWrap then P, V = '(function(...)', 'end)()' end; local B = Y .. '=' .. P
	if not
	L.shouldExport then B = "local " .. B elseif not A then D[#D + 1] = Y; B = "local " .. B
	end; R(B) R(M, Y) R(V) else local P = not L.noWrap; if P then R("do") end
R(M, L.alias or L.path) if P then R('end') end
end
end
if #D > 0 and #self.mainFiles == 0 then local L = {} for U, C in ipairs(D) do L[#L + 1] = C ..
	"=" .. C .. ", "
end; R("return {" ..
	table.concat(L) .. "}")
end
j:getChannel("end"):publish({}, self, H, E) N.close()
end

function d.Runner:Combine(_, E, T, A)
	return
	self:InjectTask(r.Factory(_, A, function(O) E:Combiner(T, O) end, r.OptionTask)):Description("Combines files into '" .. T .. "'"):Produces(T):Requires(E:Paths())
end
end
do
	function d.Runner:ListTasks(j, x) local z = {} local _ = 0
	for E, T in pairs(self.tasks) do local A = E:sub(1, 1)
	if x or
		(A ~= "_" and A ~= ".") then local O = T.description or "" local I = #E; if I > _ then _ = I end; z[E] = O
	end
	end; _ = _ + 2; j = j or "" for E, T in pairs(z) do o.WriteColor(colors.white, j .. E)
	o.PrintColor(colors.lightGray, string.rep(" ",
		_ - #E) .. T)
	end; return self
	end

	function d.Runner:Clean(j, x, z)
		return
		self:AddTask(j, z, function()
			o.Verbose("Emptying directory '" .. x .. "'")
			fs.delete(fs.combine(s.CurrentDirectory, x))
		end):Description("Clean the '" .. x .. "' directory")
	end
end
local u = e(function(j, ...) createLookup = o.CreateLookup
WhiteChars = createLookup { ' ', '\n', '\t', '\r' }
EscapeLookup = { ['\r'] = '\\r', ['\n'] = '\\n', ['\t'] = '\\t', ['"'] = '\\"', ["'"] = "\\'" }
LowerChars = createLookup { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' }
UpperChars = createLookup { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' }
Digits = createLookup { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' }
HexDigits = createLookup { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'a', 'B', 'b', 'C', 'c', 'D', 'd', 'E', 'e', 'F', 'f' }
Symbols = createLookup { '+', '-', '*', '/', '^', '%', ',', '{', '}', '[', ']', '(', ')', ';', '#' }
Keywords = createLookup { 'and', 'break', 'do', 'else', 'elseif', 'end', 'false', 'for', 'function', 'goto', 'if', 'in', 'local', 'nil', 'not', 'or', 'repeat', 'return', 'then', 'true', 'until', 'while' }
StatListCloseKeywords = createLookup { 'end', 'else', 'elseif', 'until' } UnOps = createLookup { '-', 'not', '#' }
end)
local c = e(function(j, ...) local x = u.Keywords; local z = {}
function z:AddLocal(E) table.insert(self.Locals, E) end

function z:CreateLocal(E) local T = self:GetLocal(E) if T then return T end
T = { Scope = self, Name = E, IsGlobal = false, CanRename = true, References = 1 } self:AddLocal(T) return T
end

; function z:GetLocal(E)
	for T, A in pairs(self.Locals) do if A.Name == E then return A end end
	if self.Parent then return self.Parent:GetLocal(E) end
end

; function z:GetOldLocal(E) if
self.oldLocalNamesMap[E] then return self.oldLocalNamesMap[E]
end; return
self:GetLocal(E)
end

function z:RenameLocal(E, T) E =
type(E) == 'string' and E or E.Name; local A = false
local O = self:GetLocal(E)
if O then O.Name = T; self.oldLocalNamesMap[E] = O; A = true end
if not A and self.Parent then self.Parent:RenameLocal(E, T) end
end

function z:AddGlobal(E) table.insert(self.Globals, E) end

function z:CreateGlobal(E) local T = self:GetGlobal(E) if T then return T end
T = { Scope = self, Name = E, IsGlobal = true, CanRename = true, References = 1 } self:AddGlobal(T) return T
end

; function z:GetGlobal(E)
	for T, A in pairs(self.Globals) do if A.Name == E then return A end end
	if self.Parent then return self.Parent:GetGlobal(E) end
end

function z:GetOldGlobal(E)
	if
	self.oldGlobalNamesMap[E] then return self.oldGlobalNamesMap[E]
	end; return self:GetGlobal(E)
end

function z:RenameGlobal(E, T) E = type(E) == 'string' and E or E.Name
local A = false; local O = self:GetGlobal(E) if O then O.Name = T
self.oldGlobalNamesMap[E] = O; A = true
end; if not A and self.Parent then
	self.Parent:RenameGlobal(E, T)
end
end

; function z:GetVariable(E)
	return self:GetLocal(E) or self:GetGlobal(E)
end

function z:GetOldVariable(E) return self:GetOldLocal(E) or
	self:GetOldGlobal(E)
end

function z:RenameVariable(E, T)
	E = type(E) == 'string' and E or E.Name; if self:GetLocal(E) then self:RenameLocal(E, T) else
		self:RenameGlobal(E, T)
	end
end

; function z:GetAllVariables()
	return self:getVars(true, self:getVars(true))
end

function z:getVars(E, T) local T = T or {}
if E then for A, O in
pairs(self.Children) do O:getVars(true, T)
end else for A, O in pairs(self.Locals) do
	table.insert(T, O)
end
for A, O in pairs(self.Globals) do table.insert(T, O) end
if self.Parent then self.Parent:getVars(false, T) end
end; return T
end

function z:ObfuscateLocals(E)
	local T = E or "etaoinshrdlucmfwypvbgkqjxz_ETAOINSHRDLUCMFWYPVBGKQJXZ"
	local A = E or "etaoinshrdlucmfwypvbgkqjxz_0123456789ETAOINSHRDLUCMFWYPVBGKQJXZ" local O, I = #T, #A; local N = 0; local S = math.floor
	for H, R in pairs(self.Locals) do local D
	repeat
		if N < O then N = N + 1
		D = T:sub(N, N) else
			if N < O then N = N + 1; D = T:sub(N, N) else local L = S(N / O) local U = N % O; D = T:sub(U, U) while L > 0 do U = L % I; D =
			A:sub(U, U) .. D; L = S(L / I)
			end; N = N + 1
			end
		end until not (x[D] or self:GetVariable(D)) self:RenameLocal(R.Name, D)
	end
end

; function z:ToString() return '<Scope>' end

local function _(E)
	local T = setmetatable({ Parent = E, Locals = {}, Globals = {}, oldLocalNamesMap = {}, oldGlobalNamesMap = {}, Children = {} }, { __index = z }) if E then table.insert(E.Children, T) end; return T
end

; return _
end)
local m = e(function(j, ...) local x = {} function x:Peek(z) local _ = self.tokens; z = z or 0; return
_[math.min(#_, self.pointer + z)]
end

function x:Get(z)
	local _ = self.tokens; local E = self.pointer; local T = _[E] self.pointer = math.min(E + 1, #_) if z then
		table.insert(z, T)
	end; return T
end

; function x:Is(z) return self:Peek().Type == z end

; function x:Save()
	table.insert(self.savedPointers, self.pointer)
end

function x:Commit() local z = self.savedPointers; z[#z] = nil end

function x:Restore() local z = self.savedPointers; local _ = #z; self.pointer = savedP[_] z[_] = nil end

function x:ConsumeSymbol(z, _) local E = self:Peek()
if E.Type == 'Symbol' then
	if z then if E.Data == z then self:Get(_)
	return true else return nil
	end else self:Get(_) return E
	end else return nil
end
end

function x:ConsumeKeyword(z, _) local E = self:Peek() if E.Type == 'Keyword' and E.Data == z then
	self:Get(_) return true else return nil
end
end

; function x:IsKeyword(z) local _ = self:Peek()
return _.Type == 'Keyword' and _.Data == z
end

function x:IsSymbol(z) local _ = self:Peek() return _.Type ==
	'Symbol' and _.Data == z
end

function x:IsEof() return self:Peek().Type == 'Eof' end

function x:Print(z) z = (z == nil and true or z) local _ = ""
for E, T in
ipairs(self.tokens) do if z then
	for E, A in ipairs(T.LeadingWhite) do _ = _ .. A:Print() .. "\n" end
end; _ = _ .. T:Print() .. "\n"
end; return _
end

; return x
end)
local f = e(function(j, ...) local x = o.CreateLookup; local z = u.LowerChars; local _ = u.UpperChars; local E = u.Digits
local T = u.Symbols; local A = u.HexDigits; local O = u.Keywords; local I = u.StatListCloseKeywords; local N = u.UnOps
local S = setmetatable; local H = {}
function H:Print() return
"<" .. (self.Type ..
	string.rep(' ', math.max(3, 12 - #self.Type))) .. "  " ..
	(self.Data or '') .. " >"
end

; local R = { __index = H }
local function D(U) local C = {}
do local F = 1; local W = 1; local Y = 1; local function P() local Q = U:sub(F, F)
if Q == '\n' then Y = 1; W = W + 1 else Y = Y + 1 end; F = F + 1; return Q
end

; local function V(Q) Q = Q or 0; return
U:sub(F + Q, F + Q)
end

; local function B(Q) local J = V() for X = 1, #Q do
	if J == Q:sub(X, X) then return P() end
end
end

; local function G(Q)
	error(">> :" .. W ..
		":" .. Y .. ": " .. Q, 0)
end

local function K() local Q = F
if V() == '[' then local J = 0; local X = 1
while V(J + 1) == '=' do J = J + 1 end
if V(J + 1) == '[' then for ea = 0, J + 1 do P() end; local Z = F
while true do if V() == '' then
	G("Expected `]" .. string.rep('=', J) ..
		"]` near <eof>.", 3)
end; local ea = true
if V() == ']' then for eo = 1, J do
	if V(eo) ~= '=' then ea = false end
end; if V(J + 1) ~= ']' then ea = false end else
	if V() == '[' then
		local eo = true; for ei = 1, J do if V(ei) ~= '=' then eo = false; break end end; if
		V(J + 1) == '[' and eo then X = X + 1; for ei = 1, (J + 2) do P() end
		end
	end; ea = false
end
if ea then X = X - 1; if X == 0 then break else for eo = 1, J + 2 do P() end end else P() end
end; local ee = U:sub(Z, F - 1) for ea = 0, J + 1 do P() end; local et = U:sub(Q, F - 1) return ee, et else
	return nil
end else return nil
end
end

while true do local Q = {} local J = '' local X = false
while true do local ei = V()
if ei == '#' and V(1) == '!' and W == 1 then
	P() P() J = "#!" while V() ~= '\n' and V() ~= '' do J = J .. P() end
	table.insert(Q, S({ Type = 'Comment', CommentType = 'Shebang', Data = J, Line = W, Char = Y }, R)) J = ""
end
if ei == ' ' or ei == '\t' then local en = P()
table.insert(Q, S({ Type = 'Whitespace', Line = W, Char = Y, Data = en }, R)) elseif ei == '\n' or ei == '\r' then local en = P() if J ~= "" then
	table.insert(Q, S({ Type = 'Comment', CommentType = X and 'LongComment' or 'Comment', Data = J, Line = W, Char = Y }, R)) J = ""
end
table.insert(Q, S({ Type = 'Whitespace', Line = W, Char = Y, Data = en }, R)) elseif ei == '-' and V(1) == '-' then P() P() J = J .. '--' local en, es = K()
if es then J = J .. es; X = true else while
V() ~= '\n' and V() ~= '' do J = J .. P()
end
end else break
end
end; if J ~= "" then
	table.insert(Q, S({ Type = 'Comment', CommentType = X and 'LongComment' or 'Comment', Data = J, Line = W, Char = Y }, R))
end; local Z = W; local ee = Y; local et = ":" .. W .. ":" ..
	Y .. ":> " local ea = V() local eo = nil
if ea == '' then
	eo = { Type = 'Eof' } elseif _[ea] or z[ea] or ea == '_' then local ei = F
repeat P() ea = V() until not (_[ea] or z[ea] or E[ea] or ea == '_') local en = U:sub(ei, F - 1) if O[en] then eo = { Type = 'Keyword', Data = en } else
	eo = { Type = 'Ident', Data = en }
end elseif
E[ea] or (V() == '.' and E[V(1)]) then local ei = F
if ea == '0' and V(1) == 'x' then P() P() while A[V()] do P() end; if B('Pp') then B('+-') while
E[V()] do P()
end
end else while E[V()] do P() end; if B('.') then
	while E[V()] do P() end
end
if B('Ee') then B('+-') while E[V()] do P() end end
end; eo = { Type = 'Number', Data = U:sub(ei, F - 1) } elseif
ea == '\'' or ea == '\"' then local ei = F; local en = P() local es = F
while true do local ea = P() if ea == '\\' then P() elseif ea == en then break elseif ea == '' then
	G("Unfinished string near <eof>")
end
end; local eh = U:sub(es, F - 2) local er = U:sub(ei, F - 1)
eo = { Type = 'String', Data = er, Constant = eh } elseif ea == '[' then local ei, en = K() if en then eo = { Type = 'String', Data = en, Constant = ei } else P()
eo = { Type = 'Symbol', Data = '[' }
end elseif B('>=<') then if B('=') then
	eo = { Type = 'Symbol', Data = ea .. '=' } else eo = { Type = 'Symbol', Data = ea }
end elseif B('~') then if
B('=') then eo = { Type = 'Symbol', Data = '~=' } else
	G("Unexpected symbol `~` in source.", 2)
end elseif B('.') then if B('.') then if B('.') then
	eo = { Type = 'Symbol', Data = '...' } else eo = { Type = 'Symbol', Data = '..' }
end else
	eo = { Type = 'Symbol', Data = '.' }
end elseif B(':') then if B(':') then
	eo = { Type = 'Symbol', Data = '::' } else eo = { Type = 'Symbol', Data = ':' }
end elseif T[ea] then
	P() eo = { Type = 'Symbol', Data = ea } else local ei, en = K() if ei then
	eo = { Type = 'String', Data = en, Constant = ei } else
	G("Unexpected Symbol `" .. ea .. "` in source.", 2)
end
end; eo.LeadingWhite = Q; eo.Line = Z; eo.Char = ee; C[#C + 1] = S(eo, R)
if eo.Type == 'Eof' then break end
end
end
local M = setmetatable({ tokens = C, savedPointers = {}, pointer = 1 }, { __index = m }) return M
end

local function L(U)
	local function C(Q)
		local J = ">> :" .. U:Peek().Line ..
			":" .. U:Peek().Char .. ": " .. Q .. "\n" local X = 0
		if type(src) == 'string' then
			for Z in src:gmatch("[^\n]*\n?") do if Z:sub(-1, -1) == '\n' then Z = Z:sub(1,
				-2)
			end; X = X + 1
			if X == U:Peek().Line then J = J .. ">> `" ..
				Z:gsub('\t', '    ') .. "`\n"
			for ee = 1, U:Peek().Char
			do local et = Z:sub(ee, ee) if et == '\t' then J = J .. '    ' else J = J .. ' ' end
			end; J = J .. "   ^^^^" break
			end
			end
		end; error(J)
	end

	; local M, F, W, Y, P
	local function V(Q, J) local X = c(Q) if not U:ConsumeSymbol('(', J) then
		C("`(` expected.")
	end; local Z = {} local ee = false
	while
	not U:ConsumeSymbol(')', J) do
		if U:Is('Ident') then local ea = X:CreateLocal(U:Get(J).Data)
		Z[#Z + 1] = ea
		if not U:ConsumeSymbol(',', J) then if U:ConsumeSymbol(')', J) then break else
			C("`)` expected.")
		end
		end elseif U:ConsumeSymbol('...', J) then ee = true; if not U:ConsumeSymbol(')', J) then
			C("`...` must be the last argument of a function.")
		end; break else
			C("Argument name or `...` expected")
		end
	end; local et = F(X) if not U:ConsumeKeyword('end', J) then
		C("`end` expected after function body")
	end; return
	{ AstType = 'Function', Scope = X, Arguments = Z, Body = et, VarArg = ee, Tokens = J }
	end

	function Y(Q) local J = {}
	if U:ConsumeSymbol('(', J) then local X = M(Q) if not U:ConsumeSymbol(')', J) then
		C("`)` Expected.")
	end
	return { AstType = 'Parentheses', Inner = X, Tokens = J } elseif U:Is('Ident') then local X = U:Get(J) local Z = Q:GetLocal(X.Data)
	if not Z then
		Z = Q:GetGlobal(X.Data)
		if not Z then Z = Q:CreateGlobal(X.Data) else Z.References = Z.References + 1 end else Z.References = Z.References + 1
	end; return { AstType = 'VarExpr', Name = X.Data, Variable = Z, Tokens = J } else
		C("primary expression expected")
	end
	end

	function P(Q, J) local X = Y(Q)
	while true do local Z = {}
	if U:IsSymbol('.') or U:IsSymbol(':') then
		local ee = U:Get(Z).Data
		if not U:Is('Ident') then C("<Ident> expected.") end; local et = U:Get(Z)
		X = { AstType = 'MemberExpr', Base = X, Indexer = ee, Ident = et, Tokens = Z } elseif not J and U:ConsumeSymbol('[', Z) then local ee = M(Q) if
	not U:ConsumeSymbol(']', Z) then C("`]` expected.")
	end
	X = { AstType = 'IndexExpr', Base = X, Index = ee, Tokens = Z } elseif not J and U:ConsumeSymbol('(', Z) then local ee = {}
	while
	not U:ConsumeSymbol(')', Z) do ee[#ee + 1] = M(Q)
	if not U:ConsumeSymbol(',', Z) then if
	U:ConsumeSymbol(')', Z) then break else C("`)` Expected.")
	end
	end
	end; X = { AstType = 'CallExpr', Base = X, Arguments = ee, Tokens = Z } elseif
	not J and U:Is('String') then
		X = { AstType = 'StringCallExpr', Base = X, Arguments = { U:Get(Z) }, Tokens = Z } elseif not J and U:IsSymbol('{') then local ee = W(Q)
	X = { AstType = 'TableCallExpr', Base = X, Arguments = { ee }, Tokens = Z } else break
	end
	end; return X
	end

	function W(Q) local J = {}
	if U:Is('Number') then
		return { AstType = 'NumberExpr', Value = U:Get(J), Tokens = J } elseif U:Is('String') then
		return { AstType = 'StringExpr', Value = U:Get(J), Tokens = J } elseif U:ConsumeKeyword('nil', J) then return { AstType = 'NilExpr', Tokens = J } elseif
	U:IsKeyword('false') or U:IsKeyword('true') then return
	{ AstType = 'BooleanExpr', Value = (U:Get(J).Data == 'true'), Tokens = J } elseif U:ConsumeSymbol('...', J) then return
	{ AstType = 'DotsExpr', Tokens = J } elseif U:ConsumeSymbol('{', J) then local X = {}
	local Z = { AstType = 'ConstructorExpr', EntryList = X, Tokens = J }
	while true do
		if U:IsSymbol('[', J) then U:Get(J) local ee = M(Q) if not U:ConsumeSymbol(']', J) then
			C("`]` Expected")
		end; if not U:ConsumeSymbol('=', J) then
			C("`=` Expected")
		end; local et = M(Q)
		X[#X + 1] = { Type = 'Key', Key = ee, Value = et } elseif U:Is('Ident') then local ee = U:Peek(1)
		if
		ee.Type == 'Symbol' and ee.Data == '=' then local et = U:Get(J)
		if not U:ConsumeSymbol('=', J) then C("`=` Expected") end; local ea = M(Q) X[#X + 1] = { Type = 'KeyString', Key = et.Data, Value = ea } else
			local et = M(Q) X[#X + 1] = { Type = 'Value', Value = et }
		end elseif U:ConsumeSymbol('}', J) then break else local ee = M(Q) X[#X + 1] = { Type = 'Value', Value = ee }
		end
		if U:ConsumeSymbol(';', J) or U:ConsumeSymbol(',', J) then elseif
		U:ConsumeSymbol('}', J) then break else C("`}` or table entry Expected")
		end
	end; return Z elseif U:ConsumeKeyword('function', J) then local X = V(Q, J) X.IsLocal = true; return X else return P(Q)
	end
	end

	; local B = 8
	local G = { ['+'] = { 6, 6 }, ['-'] = { 6, 6 }, ['%'] = { 7, 7 }, ['/'] = { 7, 7 }, ['*'] = { 7, 7 }, ['^'] = { 10, 9 }, ['..'] = { 5, 4 }, ['=='] = { 3, 3 }, ['<'] = { 3, 3 }, ['<='] = { 3, 3 }, ['~='] = { 3, 3 }, ['>'] = { 3, 3 }, ['>='] = { 3, 3 }, ['and'] = { 2, 2 }, ['or'] = { 1, 1 } }
	function M(Q, J) J = J or 0; local X
	if N[U:Peek().Data] then local Z = {} local ee = U:Get(Z).Data; X = M(Q, B)
	local et = { AstType = 'UnopExpr', Rhs = X, Op = ee, OperatorPrecedence = B, Tokens = Z } X = et else X = W(Q)
	end
	while true do local Z = G[U:Peek().Data]
	if Z and Z[1] > J then local ee = {}
	local et = U:Get(ee).Data; local ea = M(Q, Z[2])
	local eo = { AstType = 'BinopExpr', Lhs = X, Op = et, OperatorPrecedence = Z[1], Rhs = ea, Tokens = ee } X = eo else break
	end
	end; return X
	end

	local function K(Q) local J = nil; local X = {}
	if U:ConsumeKeyword('if', X) then local Z = {}
	local ee = { AstType = 'IfStatement', Clauses = Z }
	repeat local et = M(Q) if not U:ConsumeKeyword('then', X) then
		C("`then` expected.")
	end; local ea = F(Q)
		Z[#Z + 1] = { Condition = et, Body = ea } until not U:ConsumeKeyword('elseif', X)
	if U:ConsumeKeyword('else', X) then local et = F(Q) Z[#Z + 1] = { Body = et } end
	if not U:ConsumeKeyword('end', X) then C("`end` expected.") end; ee.Tokens = X; J = ee elseif U:ConsumeKeyword('while', X) then local Z = M(Q) if
	not U:ConsumeKeyword('do', X) then return C("`do` expected.")
	end; local ee = F(Q) if not
	U:ConsumeKeyword('end', X) then C("`end` expected.")
	end
	J = { AstType = 'WhileStatement', Condition = Z, Body = ee, Tokens = X } elseif U:ConsumeKeyword('do', X) then local Z = F(Q) if not U:ConsumeKeyword('end', X) then
		C("`end` expected.")
	end
	J = { AstType = 'DoStatement', Body = Z, Tokens = X } elseif U:ConsumeKeyword('for', X) then if not U:Is('Ident') then
		C("<ident> expected.")
	end; local Z = U:Get(X)
	if U:ConsumeSymbol('=', X) then local ee = c(Q)
	local et = ee:CreateLocal(Z.Data) local ea = M(Q)
	if not U:ConsumeSymbol(',', X) then C("`,` Expected") end; local eo = M(Q) local ei; if U:ConsumeSymbol(',', X) then ei = M(Q) end; if not
	U:ConsumeKeyword('do', X) then C("`do` expected")
	end
	local en = F(ee)
	if not U:ConsumeKeyword('end', X) then C("`end` expected") end
	J = { AstType = 'NumericForStatement', Scope = ee, Variable = et, Start = ea, End = eo, Step = ei, Body = en, Tokens = X } else local ee = c(Q) local et = { ee:CreateLocal(Z.Data) }
	while
	U:ConsumeSymbol(',', X) do
		if not U:Is('Ident') then C("for variable expected.") end; et[#et + 1] = ee:CreateLocal(U:Get(X).Data)
	end
	if not U:ConsumeKeyword('in', X) then C("`in` expected.") end; local ea = { M(Q) }
	while U:ConsumeSymbol(',', X) do ea[#ea + 1] = M(Q) end
	if not U:ConsumeKeyword('do', X) then C("`do` expected.") end; local eo = F(ee) if not U:ConsumeKeyword('end', X) then
		C("`end` expected.")
	end
	J = { AstType = 'GenericForStatement', Scope = ee, VariableList = et, Generators = ea, Body = eo, Tokens = X }
	end elseif U:ConsumeKeyword('repeat', X) then local Z = F(Q) if
	not U:ConsumeKeyword('until', X) then C("`until` expected.")
	end
	cond = M(Z.Scope) J = { AstType = 'RepeatStatement', Condition = cond, Body = Z, Tokens = X } elseif
	U:ConsumeKeyword('function', X) then
		if not U:Is('Ident') then C("Function name expected") end; local Z = P(Q, true) local ee = V(Q, X) ee.IsLocal = false; ee.Name = Z; J = ee elseif
	U:ConsumeKeyword('local', X) then
		if U:Is('Ident') then local Z = { U:Get(X).Data }
		while U:ConsumeSymbol(',', X) do
			if not
			U:Is('Ident') then C("local var name expected")
			end; Z[#Z + 1] = U:Get(X).Data
		end; local ee = {} if U:ConsumeSymbol('=', X) then repeat ee[#ee + 1] = M(Q) until
		not U:ConsumeSymbol(',', X)
		end; for et, ea in
		pairs(Z) do Z[et] = Q:CreateLocal(ea)
		end
		J = { AstType = 'LocalStatement', LocalList = Z, InitList = ee, Tokens = X } elseif U:ConsumeKeyword('function', X) then if not U:Is('Ident') then
			C("Function name expected")
		end; local Z = U:Get(X).Data
		local ee = Q:CreateLocal(Z) local et = V(Q, X) et.Name = ee; et.IsLocal = true; J = et else
			C("local var or function def expected")
		end elseif U:ConsumeSymbol('::', X) then if not U:Is('Ident') then
		C('Label name expected')
	end; local Z = U:Get(X).Data; if
	not U:ConsumeSymbol('::', X) then C("`::` expected")
	end
	J = { AstType = 'LabelStatement', Label = Z, Tokens = X } elseif U:ConsumeKeyword('return', X) then local Z = {} if not U:IsKeyword('end') then
		local ee, et = pcall(function() return M(Q) end)
		if ee then Z[1] = et; while U:ConsumeSymbol(',', X) do Z[#Z + 1] = M(Q) end end
	end
	J = { AstType = 'ReturnStatement', Arguments = Z, Tokens = X } elseif U:ConsumeKeyword('break', X) then J = { AstType = 'BreakStatement', Tokens = X } elseif
	U:ConsumeKeyword('goto', X) then if not U:Is('Ident') then C("Label expected") end
	local Z = U:Get(X).Data; J = { AstType = 'GotoStatement', Label = Z, Tokens = X } else local Z = P(Q)
	if
	U:IsSymbol(',') or U:IsSymbol('=') then if (Z.ParenCount or 0) > 0 then
		C("Can not assign to parenthesized expression, is not an lvalue")
	end; local ee = { Z } while
	U:ConsumeSymbol(',', X) do ee[#ee + 1] = P(Q)
	end; if
	not U:ConsumeSymbol('=', X) then C("`=` Expected.")
	end; local et = { M(Q) } while
	U:ConsumeSymbol(',', X) do et[#et + 1] = M(Q)
	end
	J = { AstType = 'AssignmentStatement', Lhs = ee, Rhs = et, Tokens = X } elseif Z.AstType == 'CallExpr' or Z.AstType == 'TableCallExpr' or
		Z.AstType == 'StringCallExpr' then
		J = { AstType = 'CallStatement', Expression = Z, Tokens = X } else C("Assignment Statement Expected")
	end
	end
	if U:IsSymbol(';') then J.Semicolon = U:Get(J.Tokens) end; return J
	end

	function F(Q) local J = {} local X = { Scope = c(Q), AstType = 'Statlist', Body = J, Tokens = {} }
	while not
	I[U:Peek().Data] and not U:IsEof() do local Z = K(X.Scope) J[#J + 1] = Z
	end
	if U:IsEof() then local Z = {} Z.AstType = 'Eof' Z.Tokens = { U:Get() } J[#J + 1] = Z end; return X
	end

	; return F(c())
end

; return { LexLua = D, ParseLua = L }
end)
local w = e(function(j, ...) local x = u.LowerChars; local z = u.UpperChars; local _ = u.Digits; local E = u.Symbols
local function T(N, S, H) H = H or ' '
local R, D = N:sub(-1, -1), S:sub(1, 1)
if z[R] or x[R] or R == '_' then
	if not
	(D == '_' or z[D] or x[D] or _[D]) then return N .. S else return N .. H .. S
	end elseif _[R] then
	if D == '(' then return N .. S elseif E[D] then return N .. S else return N .. H .. S end elseif R == '' then return N .. S else if D == '(' then return N .. H .. S else return N .. S end
end
end

local function A(N) local S, H; local R = 0; local function D(U, C, M)
	if R > 150 then R = 0; return U .. "\n" .. C else return T(U, C, M) end
end

H = function(U, C) local C = C or 0; local M = 0; local F = false; local W = ""
if
U.AstType == 'VarExpr' then
	if U.Variable then W = W .. U.Variable.Name else W = W .. U.Name end elseif U.AstType == 'NumberExpr' then W = W .. U.Value.Data elseif U.AstType == 'StringExpr' then W = W ..
	U.Value.Data elseif U.AstType == 'BooleanExpr' then
	W = W .. tostring(U.Value) elseif U.AstType == 'NilExpr' then W = D(W, "nil") elseif U.AstType == 'BinopExpr' then
	M = U.OperatorPrecedence; W = D(W, H(U.Lhs, M)) W = D(W, U.Op) W = D(W, H(U.Rhs)) if U.Op == '^' or U.Op ==
		'..' then M = M - 1
	end; if M < C then F = false else F = true end elseif
U.AstType == 'UnopExpr' then W = D(W, U.Op) W = D(W, H(U.Rhs)) elseif U.AstType == 'DotsExpr' then W = W ..
	"..." elseif U.AstType == 'CallExpr' then W = W .. H(U.Base) W = W .. "(" for Y = 1, #U.Arguments do W =
W .. H(U.Arguments[Y])
if Y ~= #U.Arguments then W = W .. "," end
end; W = W .. ")" elseif
U.AstType == 'TableCallExpr' then W = W .. H(U.Base) W = W .. H(U.Arguments[1]) elseif U.AstType ==
	'StringCallExpr' then W = W .. H(U.Base)
W = W .. U.Arguments[1].Data elseif U.AstType == 'IndexExpr' then W = W ..
	H(U.Base) .. "[" .. H(U.Index) .. "]" elseif U.AstType == 'MemberExpr' then W = W .. H(U.Base) .. U.Indexer ..
	U.Ident.Data elseif U.AstType ==
	'Function' then U.Scope:ObfuscateLocals() W = W .. "function("
if #U.Arguments > 0 then for Y = 1, #U.Arguments do W = W .. U.Arguments[Y].Name
if Y ~= #U.Arguments then W = W .. "," elseif U.VarArg then W = W .. ",..." end
end elseif U.VarArg then
	W = W .. "..."
end; W = W .. ")" W = D(W, S(U.Body)) W = D(W, "end") elseif
U.AstType == 'ConstructorExpr' then W = W .. "{"
for Y = 1, #U.EntryList do local P = U.EntryList[Y]
if P.Type == 'Key' then W = W .. "[" .. H(P.Key) .. "]=" ..
	H(P.Value) elseif
P.Type == 'Value' then W = W .. H(P.Value) elseif P.Type == 'KeyString' then W = W .. P.Key ..
	"=" .. H(P.Value)
end; if Y ~= #U.EntryList then W = W .. "," end
end; W = W .. "}" elseif U.AstType == 'Parentheses' then
	W = W .. "(" .. H(U.Inner) .. ")"
end; if not F then
	W = string.rep('(', U.ParenCount or 0) .. W
	W = W .. string.rep(')', U.ParenCount or 0)
end; R = R + #W; return W
end
local L = function(U) local C = ''
if U.AstType == 'AssignmentStatement' then for M = 1, #U.Lhs do C = C .. H(U.Lhs[M]) if M ~= #U.Lhs then C = C .. "," end end
if
#U.Rhs > 0 then C = C .. "=" for M = 1, #U.Rhs do C = C .. H(U.Rhs[M])
if M ~= #U.Rhs then C = C .. "," end
end
end elseif U.AstType == 'CallStatement' then C = H(U.Expression) elseif
U.AstType == 'LocalStatement' then C = C .. "local "
for M = 1, #U.LocalList do
	C = C .. U.LocalList[M].Name; if M ~= #U.LocalList then C = C .. "," end
end; if #U.InitList > 0 then C = C .. "="
for M = 1, #U.InitList do
	C = C .. H(U.InitList[M]) if M ~= #U.InitList then C = C .. "," end
end
end elseif
U.AstType == 'IfStatement' then C = D("if", H(U.Clauses[1].Condition))
C = D(C, "then") C = D(C, S(U.Clauses[1].Body))
for M = 2, #U.Clauses do
	local F = U.Clauses[M] if F.Condition then C = D(C, "elseif") C = D(C, H(F.Condition))
	C = D(C, "then") else C = D(C, "else")
	end
	C = D(C, S(F.Body))
end; C = D(C, "end") elseif U.AstType == 'WhileStatement' then
	C = D("while", H(U.Condition)) C = D(C, "do") C = D(C, S(U.Body)) C = D(C, "end") elseif
U.AstType == 'DoStatement' then C = D(C, "do") C = D(C, S(U.Body)) C = D(C, "end") elseif
U.AstType == 'ReturnStatement' then C = "return"
for M = 1, #U.Arguments do C = D(C, H(U.Arguments[M])) if M ~= #U.Arguments then C = C .. "," end end elseif U.AstType == 'BreakStatement' then C = "break" elseif U.AstType == 'RepeatStatement' then C = "repeat"
C = D(C, S(U.Body)) C = D(C, "until") C = D(C, H(U.Condition)) elseif U.AstType == 'Function' then
	U.Scope:ObfuscateLocals() if U.IsLocal then C = "local" end; C = D(C, "function ") if U.IsLocal then
		C = C .. U.Name.Name else C = C .. H(U.Name)
	end; C = C .. "("
	if
	#U.Arguments > 0 then
		for M = 1, #U.Arguments do C = C .. U.Arguments[M].Name; if
		M ~= #U.Arguments then C = C .. "," elseif U.VarArg then C = C .. ",..."
		end
		end elseif U.VarArg then C = C .. "..."
	end; C = C .. ")" C = D(C, S(U.Body)) C = D(C, "end") elseif
U.AstType == 'GenericForStatement' then U.Scope:ObfuscateLocals() C = "for " for M = 1, #U.VariableList do C = C ..
	U.VariableList[M].Name
if M ~= #U.VariableList then C = C .. "," end
end; C = C .. " in"
for M = 1, #U.Generators do
	C = D(C, H(U.Generators[M])) if M ~= #U.Generators then C = D(C, ',') end
end; C = D(C, "do") C = D(C, S(U.Body)) C = D(C, "end") elseif
U.AstType == 'NumericForStatement' then U.Scope:ObfuscateLocals() C = "for "
C = C .. U.Variable.Name .. "=" C = C .. H(U.Start) .. "," .. H(U.End) if U.Step then C = C .. "," ..
	H(U.Step)
end; C = D(C, "do")
C = D(C, S(U.Body)) C = D(C, "end") elseif U.AstType == 'LabelStatement' then
	C = "::" .. U.Label .. "::" elseif U.AstType == 'GotoStatement' then C = "goto " .. U.Label elseif U.AstType == 'Comment' then elseif U.AstType ==
	'Eof' then else
	error("Unknown AST Type: " .. U.AstType)
end; R = R + #C; return C
end
S = function(U) local C = '' U.Scope:ObfuscateLocals() for M, F in pairs(U.Body) do
	C = D(C, L(F), ';')
end; return C
end; return S(N)
end

; local function O(N) local S = f.LexLua(N) t.refreshYield() S = f.ParseLua(S)
t.refreshYield() return A(S)
end

local function I(N, S) S = S or N
local H = s.CurrentDirectory; local R = fs.open(fs.combine(H, N), "r") local D = R.readAll()
R.close() D = O(D) local L = fs.open(fs.combine(H, S), "w") L.write(D)
L.close()
end

; return { JoinStatements = T, Minify = A, MinifyString = O, MinifyFile = I }
end)
do local j = w.MinifyFile; local x = function(z, _, E) return j(_, E) end
function d.Runner:Minify(z, _, E, T)
	return
	self:AddTask(z, T, function()
		if
		type(_) == "table" then
			assert(type(E) == "table", "Output File must be a table too") local A = #_
			assert(A == #E, "Tables must be the same length") for O = 1, A do j(_[O], E[O]) end else j(_, E)
		end
	end):Description("Minifies '" ..
		fs.getName(_) .. "' into '" .. fs.getName(E) .. "'"):Requires(_):Produces(E)
end

function d.Runner:MinifyAll(z, _, E) z = z or "_minify" return
self:AddTask(z, {}, x):Description("Minifies files"):Maps(_ or "wild:*.lua", E or "wild:*.min.lua")
end

i.Subscribe({ "HowlFile", "env" }, function(z) z.Minify = j end)
end
local y = e(function(j, ...) local x = {} function x:Add(_)
	table.insert(self.include, self:_Parse(_)) self.files = nil; return self
end

; function x:Remove(_)
	table.insert(self.exclude, self:_Parse(_)) self.files = nil; return self
end

x.Include = x.Add; x.Exclude = x.Remove
function x:Files()
	if not self.files then self.files = {}
	for E, T in ipairs(self.include) do if
	T.Type == "Normal" then self:_Include(T.Text) else
		self:_Include("", T.Text)
	end
	end
	end; return self.files
end

function x:_Include(_, E) if _ ~= "" then
	for A, E in pairs(self.exclude) do if E.Match(_) then return end end
end
local T = fs.combine(self.path, _)
assert(fs.exists(T), "Cannot find path " .. _) if fs.isDir(T) then for A, O in ipairs(fs.list(T)) do
	self:_Include(fs.combine(_, O), E)
end elseif not E or E:match(_) then
	self.files[_] = true
end
end

function x:_Parse(_) _ = o.ParsePattern(_) local E = _.Text
if o.Type == "Normal" then
	function _.Match(T) return E == T end else function _.Match(T) return T:match(E) end
end; return _
end

local function z(_) return
setmetatable({ path = _ or s.CurrentDirectory, include = {}, exclude = {}, startup = 'startup' }, { __index = x })
end

i.Subscribe({ "HowlFile", "env" }, function(_) _.Files = z end) return { Files = x, Factory = z }
end)
do
	local j = [=[--[[Hideously Smashed Together by Compilr, a Hideous Smash-Stuff-Togetherer, (c) 2014 oeed
	This file REALLLLLLLY isn't suitable to be used for anything other than being executed
	To extract all the files, run: "<filename> --extract" in the Shell
]]
]=]
	local x = [[
local function run(tArgs)
	local fnFile, err = loadstring(files[%q], %q)
	if err then error(err) end

	local function split(str, pat)
		 local t = {}
		 local fpat = "(.-)" .. pat
		 local last_end = 1
		 local s, e, cap = str:find(fpat, 1)
		 while s do
				if s ~= 1 or cap ~= "" then
		 table.insert(t,cap)
				end
				last_end = e+1
				s, e, cap = str:find(fpat, last_end)
		 end
		 if last_end <= #str then
				cap = str:sub(last_end)
				table.insert(t, cap)
		 end
		 return t
	end

	local function resolveTreeForPath(path, single)
		local _files = files
		local parts = split(path, '/')
		if parts then
			for i, v in ipairs(parts) do
				if #v > 0 then
					if _files[v] then
						_files = _files[v]
					else
						_files = nil
						break
					end
				end
			end
		elseif #path > 0 and path ~= '/' then
			_files = _files[path]
		end
		if not single or type(_files) == 'string' then
			return _files
		end
	end

	local oldFs = fs
	local env
	env = {
		fs = {
			list = function(path)
							local list = {}
							if fs.exists(path) then
						list = fs.list(path)
							end
				for k, v in pairs(resolveTreeForPath(path)) do
					if not fs.exists(path .. '/' ..k) then
						table.insert(list, k)
					end
				end
				return list
			end,

			exists = function(path)
				if fs.exists(path) then
					return true
				elseif resolveTreeForPath(path) then
					return true
				else
					return false
				end
			end,

			isDir = function(path)
				if fs.isDir(path) then
					return true
				else
					local tree = resolveTreeForPath(path)
					if tree and type(tree) == 'table' then
						return true
					else
						return false
					end
				end
			end,

			isReadOnly = function(path)
				if not fs.isReadOnly(path) then
					return false
				else
					return true
				end
			end,

			getName = fs.getName,
			getSize = fs.getSize,
			getFreespace = fs.getFreespace,
			makeDir = fs.makeDir,
			move = fs.move,
			copy = fs.copy,
			delete = fs.delete,
			combine = fs.combine,

			open = function(path, mode)
				if fs.exists(path) then
					return fs.open(path, mode)
				elseif type(resolveTreeForPath(path)) == 'string' then
					local handle = {close = function()end}
					if mode == 'r' then
						local content = resolveTreeForPath(path)
						handle.readAll = function()
							return content
						end

						local line = 1
						local lines = split(content, '\n')
						handle.readLine = function()
							if line > #lines then
								return nil
							else
								return lines[line]
							end
							line = line + 1
						end
											return handle
					else
						error('Cannot write to read-only file (compilr archived).')
					end
				else
					return fs.open(path, mode)
				end
			end
		},

		loadfile = function( _sFile )
				local file = env.fs.open( _sFile, "r" )
				if file then
						local func, err = loadstring( file.readAll(), fs.getName( _sFile ) )
						file.close()
						return func, err
				end
				return nil, "File not found: ".._sFile
		end,

		dofile = function( _sFile )
				local fnFile, e = env.loadfile( _sFile )
				if fnFile then
						setfenv( fnFile, getfenv(2) )
						return fnFile()
				else
						error( e, 2 )
				end
		end
	}

	setmetatable( env, { __index = _G } )

	local tAPIsLoading = {}
	env.os.loadAPI = function( _sPath )
			local sName = fs.getName( _sPath )
			if tAPIsLoading[sName] == true then
					printError( "API "..sName.." is already being loaded" )
					return false
			end
			tAPIsLoading[sName] = true

			local tEnv = {}
			setmetatable( tEnv, { __index = env } )
			local fnAPI, err = env.loadfile( _sPath )
			if fnAPI then
					setfenv( fnAPI, tEnv )
					fnAPI()
			else
					printError( err )
					tAPIsLoading[sName] = nil
					return false
			end

			local tAPI = {}
			for k,v in pairs( tEnv ) do
					tAPI[k] =  v
			end

			env[sName] = tAPI
			tAPIsLoading[sName] = nil
			return true
	end

	env.shell = shell

	setfenv( fnFile, env )
	fnFile(unpack(tArgs))
end

local function extract()
		local function node(path, tree)
				if type(tree) == 'table' then
						fs.makeDir(path)
						for k, v in pairs(tree) do
								node(path .. '/' .. k, v)
						end
				else
						local f = fs.open(path, 'w')
						if f then
								f.write(tree)
								f.close()
						end
				end
		end
		node('', files)
end

local tArgs = {...}
if #tArgs == 1 and tArgs[1] == '--extract' then
	extract()
else
	run(tArgs)
end
]]
	function y.Files:Compilr(z, _) local E = self.path; _ = _ or {} local T = self:Files() if
	not T[self.startup] then
		error('You must have a file called ' .. self.startup .. ' to be executed at runtime.')
	end; local A = {}
	for N, S in pairs(T) do
		local H = fs.open(fs.combine(E, N), "r") local R = H.readAll() H.close() if _.minify and loadstring(R) then
			R = w.MinifyString(R)
		end; local D = A
		local L = { N:match((N:gsub("[^/]+/?", "([^/]+)/?"))) } L[#L] = nil
		for S, U in pairs(L) do local C = D[U] if not C then C = {} D[U] = C end; D = C end; D[fs.getName(N)] = R
	end
	local O = j .. "local files = " .. t.serialize(A) ..
		"\n" .. string.format(x, self.startup, self.startup) if _.minify then O = w.MinifyString(O) end
	local I = fs.open(fs.combine(s.CurrentDirectory, z), "w") I.write(O) I.close()
	end

	function d.Runner:Compilr(z, _, E, T) return
	self:AddTask(z, T, function() _:Compilr(E) end):Description("Combines multiple files using Compilr"):Produces(E)
	end
end
do local j, x, z, _, E = fs.combine, fs.exists, fs.isDir, loadfile, o.Verbose; local T = busted
local A = { "busted.api.lua", "../lib/busted.api.lua", "busted.api", "../lib/busted.api", "busted", "../lib/busted" }
local function O(H) E("Busted at " .. H) local R = _(H)
if R then
	E("Busted loading at " .. H) local D = setfenv(R, getfenv())() D = D.api or D; if D.run then E("Busted found at " .. H) return D end
end
end

local function I(H) if not x(H) then return end; if not z(H) then return O(H) end; local R
for D, L in ipairs(A) do R = j(H, L) if
x(R) then local U = O(R) if U then return U end
end
end
end

local function N() if T then return T end; local H = I("/") if H then T = H; return T end; for R in
string.gmatch(shell.path(), "[^:]+") do local H = I(R) if H then T = H; return T end
end
end

local function S()
	return
	{ cwd = s.CurrentDirectory, output = 'colorTerminal', seed = os.time(), verbose = o.IsVerbose(), root = 'spec', tags = {}, ['exclude-tags'] = {}, pattern = '_spec', loaders = { 'lua' }, helper = '' }
end

function d.Runner:Busted(H, R, D)
	return
	self:AddTask(H, D, function() local T
	if R and R.busted then T = I(R.busted) else T = N() end; if not T then error("Cannot find busted") end; local L = S() for M, F in pairs(R or
		{}) do L[M] = F
	end; local U, C = T.run(L, S())
	if U ~= 0 then
		o.VerboseLog(messages) error("Not all tests passed")
	end
	end):Description("Runs tests")
end
end; local p = n.Options({ ... }) local v = d.Factory() local b = p:Arguments()
i.Subscribe({ "ArgParse", "changed" }, function(p) o.IsVerbose(p:Get("verbose") or false)
v.ShowTime = p:Get "time" v.Traceback = p:Get "trace" if p:Get "help" then b = { "help" } end
end)
p:Option "verbose":Alias "v":Description "Print verbose output"
p:Option "time":Alias "t":Description "Display the time taken for tasks"
p:Option "trace":Description "Print a stack trace on errors"
p:Option "help":Alias "?":Alias "h":Description "Print this help" local g, k = s.FindHowl()
if not g then
	if p:Get("help") or
		(#b == 1 and b[1] == "help") then o.PrintColor(colors.yellow, "Howl")
	o.PrintColor(colors.lightGrey, "Howl is a simple build system for Lua")
	o.PrintColor(colors.grey, "You can read the full documentation online: https://github.com/SquidDev-CC/Howl/wiki/")
	o.PrintColor(colors.white, (([[
			The key thing you are missing is a HowlFile. This can be "Howlfile" or "Howlfile.lua".
			Then you need to define some tasks. Maybe something like this:
		]]):gsub("\t", "")))
	o.PrintColor(colors.pink, 'Tasks:Minify("minify", "Result.lua", "Result.min.lua")')
	o.PrintColor(colors.white, "Now just run `Howl minify`!")
	end; error(k, 0)
end; s.CurrentDirectory = k
o.Verbose("Found HowlFile at " .. fs.combine(k, g))
v:Task "list"(function() v:ListTasks() end):Description "Lists all the tasks"
v:Task "help"(function() o.Print("Howl [options] [task]")
o.PrintColor(colors.orange, "Tasks:") v:ListTasks("  ")
o.PrintColor(colors.orange, "\nOptions:") p:Help("  ")
end):Description "Print out a detailed usage for Howl"
v:Default(function() o.PrintError("No default task exists.")
o.Verbose("Use 'Tasks:Default' to define a default task") o.PrintColor(colors.orange, "Choose from: ")
v:ListTasks("  ")
end)
local q = s.SetupEnvironment({ CurrentDirectory = k, Tasks = v, Options = p, Verbose = o.Verbose, Log = o.VerboseLog, File = function(...) return fs.combine(k, ...) end }) q.dofile(fs.combine(k, g)) v:RunMany(b)
