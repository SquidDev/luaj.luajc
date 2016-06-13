term.setBackgroundColor(1)
term.setTextColor(32768)
term.clear()
if gui == 1 then
	local sidebar = 1
	while sidebar < 51 do
		term.setCursorPos(1, sidebar)
		term.setBackgroundColor(8)
		term.write("                ")
		sidebar = sidebar + 1
	end
	term.setCursorPos(2, 2)
	drawKrist()
	term.setBackgroundColor(8)
	term.setTextColor(32768)
	term.write(" KristWallet")
	term.setCursorPos(5, 3)
	term.setTextColor(2048)
	term.write("release " .. version .. "")
	term.setCursorPos(2, 19)
	term.write("    by 3d6")
	term.setTextColor(32768)
	term.setCursorPos(3, 5)
	drawTab("  Overview  ")
	term.setCursorPos(3, 7)
	drawTab("Transactions")
	term.setCursorPos(3, 9)
	drawTab(" Send Krist ")
	term.setCursorPos(3, 11)
	drawTab(" Special TX ")
	term.setCursorPos(3, 13)
	drawTab(" Economicon ")
	term.setCursorPos(3, 15)
	drawTab("Name Manager")
	term.setCursorPos(3, 17)
	drawTab("    Exit    ")
	term.setBackgroundColor(1)
elseif gui == 2 then
	term.setCursorPos(1, 1)
	term.setBackgroundColor(8)
	term.write("                          ")
	term.setCursorPos(1, 2)
	term.write("                          ")
	term.setCursorPos(1, 3)
	term.write("                          ")
	term.setCursorPos(1, 4)
	term.write("                          ")
	term.setCursorPos(2, 2)
	drawKrist()
	term.setBackgroundColor(8)
	term.setTextColor(32768)
	term.write(" KristWallet")
	term.setCursorPos(5, 3)
	term.setTextColor(2048)
	term.write("release " .. version .. "")
	term.setCursorPos(19, 2)
	term.setBackgroundColor(16384)
	term.setTextColor(32768)
	term.write(" Exit ")
end
if page == 1 then
	term.setCursorPos(19, 2)
	term.write("Your address: ")
	term.setTextColor(16384)
	term.write(address)
	term.setTextColor(32768)
	term.setCursorPos(19, 5)
	local recenttransactions = ""
	if tostring(balance) ~= 'nil' then recenttransactions = readURL(readconfig("syncnode") .. "?listtx=" .. address .. "&overview") end
	local txtype = 0
	local graphics = 0
	if string.len(recenttransactions) > 25 then
		repeat
			if string.sub(recenttransactions, 13 + (31 * graphics), 22 + (31 * graphics)) == "N/A(Mined)" then txtype = 1
			elseif string.sub(recenttransactions, 13 + (31 * graphics), 22 + (31 * graphics)) == "N/A(Names)" and tonumber(string.sub(recenttransactions, 23 + (31 * graphics), 31 + (31 * graphics))) == 0 then txtype = 7
			elseif tonumber(string.sub(recenttransactions, 23 + (31 * graphics), 31 + (31 * graphics))) == 0 then txtype = 9
			elseif string.sub(recenttransactions, 13 + (31 * graphics), 22 + (31 * graphics)) == "N/A(Names)" then txtype = 6
			elseif string.sub(recenttransactions, 13 + (31 * graphics), 22 + (31 * graphics)) == address then txtype = 4
			elseif string.sub(recenttransactions, 13 + (31 * graphics), 22 + (31 * graphics)) == addressv1 then txtype = 5
			elseif tonumber(string.sub(recenttransactions, 23 + (31 * graphics), 31 + (31 * graphics))) < 0 then txtype = 2
			elseif tonumber(string.sub(recenttransactions, 23 + (31 * graphics), 31 + (31 * graphics))) > 0 then txtype = 3
			else txtype = 8
			end
			postgraphic(19, 5 + (5 * graphics), txtype)
			term.setCursorPos(26, 5 + (5 * graphics))
			term.setBackgroundColor(1)
			term.setTextColor(32768)
			if txtype == 1 then term.write("Mined")
			elseif txtype == 2 then term.write("Sent")
			elseif txtype == 3 then term.write("Received")
			elseif txtype == 4 then term.write("Sent to yourself")
			elseif txtype == 5 then term.write("Imported")
			elseif txtype == 6 then term.write("Name registered")
			elseif txtype == 7 then term.write("Name operation")
			elseif txtype == 8 then term.write("Unknown")
			elseif txtype == 9 then term.write("Name transfer")
			end
			term.setCursorPos(26, 6 + (5 * graphics))
			if txtype == 4 then
				term.setTextColor(32768)
			elseif tonumber(string.sub(recenttransactions, 23 + (31 * graphics), 31 + (31 * graphics))) > 0 then
				term.setTextColor(8192)
				term.write("+")
			elseif tonumber(string.sub(recenttransactions, 23 + (31 * graphics), 31 + (31 * graphics))) == 0 then
				term.setTextColor(16)
			else
				term.setTextColor(16384)
			end
			if txtype < 7 then term.write(tostring(tonumber(string.sub(recenttransactions, 23 + (31 * graphics), 31 + (31 * graphics)))) .. " KST") end
			term.setCursorPos(26, 7 + (5 * graphics))
			term.setTextColor(32768)
			if txtype ~= 6 then term.setTextColor(512) end
			if txtype == 9 or (txtype > 1 and txtype < 6) then term.write(string.sub(recenttransactions, 13 + (31 * graphics), 22 + (31 * graphics))) end
			--if txtype == 6 then term.write(".kst") end
			term.setCursorPos(26, 8 + (5 * graphics))
			term.setTextColor(128)
			term.write(string.sub(recenttransactions, 1 + (31 * graphics), 12 + (31 * graphics)))
			graphics = graphics + 1
		until graphics >= math.floor(string.len(recenttransactions) / 32)
	end
	term.setTextColor(32768)
	term.setCursorPos(19, 3)
	term.write("Your balance: ")
	term.setTextColor(1024)
	if tostring(balance) == 'nil' then balance = 0 end
	term.write(tostring(balance) .. " KST ")
	term.setTextColor(512)
	local names = tonumber(readURL(readconfig("syncnode") .. "?getnames=" .. address))
	if names > 0 then term.write("[" .. tostring(names) .. "]") end
	if address == "ke3kjplzsz" or address == "767fc628a4" or address == "e3b0c44298" then
		term.setCursorPos(1, 1)
		term.setBackgroundColor(16384)
		term.setTextColor(16)
		term.clearLine()
		term.write("You are currently using a blank string password.")
	end
elseif page == 2 then
	term.setCursorPos(18, 1)
	term.write("Please wait...")
	os.sleep(0)
	subbal = readURL(readconfig("syncnode") .. "?getbalance=" .. subject)
	subtxs = readURL(readconfig("syncnode") .. "?listtx=" .. subject)
	log("Loaded transactions for address " .. subject)
	log("Page index is " .. scroll)
	term.setCursorPos(18, 1)
	if subtxs == "end" then subbal = 0 end
	term.write("ADDRESS " .. subject .. " - " .. subbal .. " KST")
	term.setCursorPos(17, 2)
	term.setBackgroundColor(256)
	term.write(" Time         Peer           Value ")
	term.setBackgroundColor(1)
	if subtxs ~= "end" then
		local tx = 0
		local s = 0
		ar = 16 * scroll
		repeat
			tx = tx + 1
			stdate[tx] = string.sub(subtxs, 1, 12)
			subtxs = string.sub(subtxs, 13)
			stpeer[tx] = string.sub(subtxs, 1, 10)
			subtxs = string.sub(subtxs, 11)
			stval[tx] = tonumber(string.sub(subtxs, 1, 9))
			subtxs = string.sub(subtxs, 10)
			if stpeer[tx] == subject then stval[tx] = 0 end
		until string.len(subtxs) == 3
		repeat
			ar = ar + 1
			term.setTextColor(32768)
			term.setCursorPos(18, 2 + ar - (16 * (scroll)))
			term.write(stdate[ar])
			if stpeer[ar] ~= "N/A(Mined)" then term.setTextColor(512) end
			if stpeer[ar] == subject then term.setTextColor(32768) end
			if stpeer[ar] == "N/A(Names)" then term.setTextColor(32768) end
			term.setCursorPos(31, 2 + ar - (16 * (scroll)))
			term.write(stpeer[ar])
			term.setCursorPos(50 - string.len(tostring(math.abs(stval[ar]))), 2 + ar - (16 * (scroll)))
			if stval[ar] > 0 then
				term.setTextColor(8192)
				term.write("+")
			elseif stval[ar] < 0 then
				term.setTextColor(16384)
			else
				term.setTextColor(32768)
				term.write(" ")
			end
			term.write(tostring(stval[ar]))
		until ar == math.min(tx, 16 * (scroll + 1))
		term.setBackgroundColor(256)
		term.setCursorPos(17, 19)
		term.write("                                   ")
		term.setCursorPos(17, 19)
		term.setTextColor(32768)
		lastpage = math.floor((tx - 1) / 16)
		if (1 + lastpage) < 100 then maxspace = maxspace .. " " end
		if (1 + lastpage) < 10 then maxspace = maxspace .. " " end
		if (1 + scroll) < 100 then pagespace = pagespace .. " " end
		if (1 + scroll) < 10 then pagespace = pagespace .. " " end
		term.write(" Page " .. pagespace .. (1 + scroll) .. "/" .. maxspace .. (1 + lastpage))
		pagespace = ""
		maxspace = ""
		term.setCursorPos(32, 19)
		term.setTextColor(128)
		term.write("First Prev Next Last")
		if (scroll > 0) then
			term.setCursorPos(32, 19)
			term.setTextColor(2048)
			term.write("First Prev")
		end
		if (scroll < lastpage and tx > 16) then
			term.setCursorPos(43, 19)
			term.setTextColor(2048)
			term.write("Next Last")
		end
	else
		term.write("No transactions to display!")
		term.setBackgroundColor(256)
		term.setCursorPos(17, 19)
		term.write("                                   ")
		term.setCursorPos(17, 19)
		term.setTextColor(32768)
		term.write(" Page   1/  1")
		term.setCursorPos(32, 19)
		term.setTextColor(128)
		term.write("First Prev Next Last")
	end
elseif page == 3 then
	term.setCursorPos(19, 2)
	term.write("Your address: ")
	term.setTextColor(16384)
	term.write(address)
	term.setTextColor(32768)
	term.setCursorPos(19, 3)
	term.write("Your balance: ")
	term.setTextColor(1024)
	if tostring(balance) == 'nil' then balance = 0 end
	term.write(tostring(balance) .. " KST")
	term.setTextColor(32768)
	term.setCursorPos(19, 5)
	term.write("Recipient:    ")
	term.write("                   ")
	term.setCursorPos(19, 6)
	term.write("Amount (KST): ")
	term.write("                   ")
elseif page == 4 then
	term.setCursorPos(19, 2)
	term.write("Mining          Addresses")
	term.setTextColor(512)
	term.setCursorPos(19, 3)
	term.write("Latest blocks   Address lookup")
	term.setCursorPos(19, 4)
	term.write("Lowest hashes   Top balances")
	term.setCursorPos(19, 5)
	--term.write("Lowest nonces   ")
	term.setTextColor(32768)
	term.setCursorPos(19, 7)
	--term.write("Economy         Transactions")
	term.setTextColor(512)
	term.setCursorPos(19, 8)
	--term.write("KST issuance    Latest transfers")
	term.setCursorPos(19, 9)
	--term.write("KST distrib.    Largest transfers")
elseif page == 5 then
	local blocks = readURL(readconfig("syncnode") .. "?blocks")
	local tx = 0
	ar = 0
	local height = string.sub(blocks, 1, 8)
	local blktime = {}
	blkpeer = {}
	local blkhash = {}
	height = tonumber(string.sub(blocks, 1, 8))
	blocks = string.sub(blocks, 9)
	local today = string.sub(blocks, 1, 10)
	blocks = string.sub(blocks, 11)
	repeat
		tx = tx + 1
		blktime[tx] = string.sub(blocks, 1, 8)
		blocks = string.sub(blocks, 9)
		blkpeer[tx] = string.sub(blocks, 1, 10)
		blocks = string.sub(blocks, 11)
		blkhash[tx] = string.sub(blocks, 1, 12)
		blocks = string.sub(blocks, 13)
		if stpeer[tx] == subject then stval[tx] = 0 end
	until string.len(blocks) == 0
	term.setCursorPos(18, 1)
	term.write("Height: " .. tostring(height))
	term.setCursorPos(36, 1)
	term.write("Date: " .. today)
	term.setCursorPos(17, 2)
	term.setBackgroundColor(256)
	term.write(" Time     Miner      Hash          ")
	---------- (" 00:00:00 0000000000 000000000000 ")
	term.setBackgroundColor(1)
	repeat
		ar = ar + 1
		term.setCursorPos(18, 2 + ar)
		term.write(blktime[ar])
		if blkpeer[ar] ~= "N/A(Burnt)" then term.setTextColor(512) end
		term.setCursorPos(27, 2 + ar)
		term.write(blkpeer[ar])
		term.setTextColor(32768)
		term.setCursorPos(38, 2 + ar)
		term.write(blkhash[ar])
	until ar == math.min(tx, 17 * (scroll + 1))
elseif page == 6 then
	term.setCursorPos(17, 2)
	term.setBackgroundColor(256)
	term.write(" Time         Peer           Value ")
	term.setBackgroundColor(256)
	term.setCursorPos(17, 19)
	term.write("                                   ")
	term.setCursorPos(17, 19)
	term.setTextColor(32768)
	term.write(" Page    /")
	term.setCursorPos(32, 19)
	term.setTextColor(128)
	term.write("First Prev Next Last")
	term.setBackgroundColor(1)
	term.setCursorPos(18, 1)
	term.write("ADDRESS (click to edit)")
elseif page == 7 then
	local blocks = readURL(readconfig("syncnode") .. "?richapi")
	local tx = 0
	ar = 0
	local height = string.sub(blocks, 1, 8)
	local blktime = {}
	blkpeer = {}
	local blkhash = {}
	repeat
		tx = tx + 1
		blkpeer[tx] = string.sub(blocks, 1, 10)
		blocks = string.sub(blocks, 11)
		blktime[tx] = tonumber(string.sub(blocks, 1, 8))
		blocks = string.sub(blocks, 9)
		blkhash[tx] = string.sub(blocks, 1, 11)
		blocks = string.sub(blocks, 12)
	until string.len(blocks) == 0
	term.setCursorPos(18, 1)
	term.write("Krist address rich list")
	term.setCursorPos(17, 2)
	term.setBackgroundColor(256)
	term.write("R# Address     Balance First seen  ")
	term.setBackgroundColor(1)
	repeat
		ar = ar + 1
		term.setCursorPos(17, 2 + ar)
		if ar < 10 then term.write(" ") end
		term.write(ar)
		term.setCursorPos(20, 2 + ar)
		if blkpeer[ar] ~= "N/A(Burnt)" then term.setTextColor(512) end
		term.write(blkpeer[ar])
		term.setTextColor(32768)
		term.setCursorPos(39 - string.len(tostring(math.abs(blktime[ar]))), 2 + ar)
		term.write(blktime[ar])
		term.setCursorPos(40, 2 + ar)
		term.write(blkhash[ar])
	until ar == 16
elseif page == 8 then
	term.setCursorPos(19, 2)
	term.write("Storage         Names")
	term.setTextColor(512)
	term.setCursorPos(19, 3)
	term.write("Double vault    Register name")
	term.setCursorPos(19, 4)
	term.write("Local vault")
	term.setCursorPos(19, 5)
	--term.write("Disk vault      v1 SHA vault")
	term.setCursorPos(19, 6)
	--term.write("SHA vault       v1 wallet")
elseif page == 9 then
	term.setCursorPos(25, 2)
	term.write("Double vault manager")
	term.setCursorPos(19, 8)
	term.write("Using double vaults is a way to")
	term.setCursorPos(19, 9)
	term.write("store your Krist under an extra")
	term.setCursorPos(19, 10)
	term.write("layer of security. You can only")
	term.setCursorPos(19, 11)
	term.write("access a double vault from your")
	term.setCursorPos(19, 12)
	term.write("wallet (on any server) and then")
	term.setCursorPos(19, 13)
	term.write("only after typing an extra pass")
	term.setCursorPos(19, 14)
	term.write("code. Double wallets are wholly")
	term.setCursorPos(19, 15)
	term.write("invisible to unauthorized users")
	term.setCursorPos(19, 16)
	term.write("of your wallet; they can not be")
	term.setCursorPos(19, 17)
	term.write("seen or opened without the pass")
	term.setCursorPos(19, 18)
	term.write("code set by you.")
	term.setCursorPos(19, 4)
	term.write("Pass code: ")
	term.setCursorPos(19, 5)
	term.write("Amount (KST): ")
	term.setCursorPos(30, 4)
	if string.len(doublekey) == 0 then
		term.setTextColor(256)
		term.write("(click to set)")
	else
		term.setTextColor(8192)
		term.write("Ready: " .. balance2 .. " KST")
		if tonumber(amt) > 0 then
			term.setCursorPos(25, 6)
			term.setTextColor(32768)
			term.setBackgroundColor(128)
			if tonumber(amt) <= balance then
				term.setBackgroundColor(2)
			end
			term.write(" Deposit ")
			term.setBackgroundColor(1)
			term.write(" ")
			term.setBackgroundColor(128)
			if tonumber(amt) <= balance2 then
				term.setBackgroundColor(2)
			end
			term.write(" Withdraw ")
			term.setBackgroundColor(1)
		end
	end
	term.setCursorPos(33, 5)
	if amt == 0 then
		term.setTextColor(256)
		term.write("(click to set)")
	else
		term.setTextColor(32768)
		term.write(amt)
	end
	term.setTextColor(32768)
elseif page == 10 then
	local blocks = readURL(readconfig("syncnode") .. "?blocks&low")
	local tx = 0
	ar = 0
	local blktime = {}
	blkpeer = {}
	local blkhash = {}
	repeat
		tx = tx + 1
		blktime[tx] = string.sub(blocks, 1, 6)
		blocks = string.sub(blocks, 7)
		blkpeer[tx] = string.sub(blocks, 1, 6)
		blocks = string.sub(blocks, 7)
		blkhash[tx] = string.sub(blocks, 1, 20)
		blocks = string.sub(blocks, 21)
	until string.len(blocks) == 0
	term.setCursorPos(17, 1)
	term.setBackgroundColor(256)
	term.write(" Date   Block# Hash                ")
	---------- (" Feb 28 000000 000000000000oooooooo")
	term.setBackgroundColor(1)
	repeat
		ar = ar + 1
		term.setCursorPos(18, 1 + ar)
		term.write(blktime[ar])
		term.setCursorPos(31 - string.len(tostring(math.abs(tonumber(blkpeer[ar])))), 1 + ar)
		term.write(tonumber(blkpeer[ar]))
		term.setTextColor(256)
		term.setCursorPos(32, 1 + ar)
		term.write(blkhash[ar])
		term.setTextColor(32768)
		term.setCursorPos(32, 1 + ar)
		term.write(string.sub(blkhash[ar], 1, 12))
	until ar == math.min(tx, 18)
elseif page == 11 then
	local blocks = readURL(readconfig("syncnode") .. "?blocks&low&lownonce")
	local tx = 0
	ar = 0
	local blktime = {}
	blkpeer = {}
	local blkhash = {}
	repeat
		tx = tx + 1
		blktime[tx] = string.sub(blocks, 1, 6)
		blocks = string.sub(blocks, 7)
		blkpeer[tx] = string.sub(blocks, 1, 6)
		blocks = string.sub(blocks, 7)
		blkhash[tx] = string.sub(blocks, 1, 12)
		blocks = string.sub(blocks, 13)
	until string.len(blocks) == 0
	term.setCursorPos(17, 1)
	term.setBackgroundColor(256)
	term.write(" Date   Block# Nonce               ")
	---------- (" Feb 28 000000 000000000000")
	term.setBackgroundColor(1)
	repeat
		ar = ar + 1
		term.setCursorPos(18, 1 + ar)
		term.write(blktime[ar])
		term.setCursorPos(31 - string.len(tostring(math.abs(tonumber(blkpeer[ar])))), 1 + ar)
		term.write(tonumber(blkpeer[ar]))
		term.setTextColor(32768)
		term.setCursorPos(32, 1 + ar)
		term.write(tonumber(blkhash[ar]))
	until ar == math.min(tx, 18)
elseif page == 12 then
	local blocks = readURL(readconfig("syncnode") .. "?blocks&low&highnonce")
	local tx = 0
	ar = 0
	local blktime = {}
	blkpeer = {}
	local blkhash = {}
	repeat
		tx = tx + 1
		blktime[tx] = string.sub(blocks, 1, 6)
		blocks = string.sub(blocks, 7)
		blkpeer[tx] = string.sub(blocks, 1, 6)
		blocks = string.sub(blocks, 7)
		blkhash[tx] = string.sub(blocks, 1, 12)
		blocks = string.sub(blocks, 13)
	until string.len(blocks) == 0
	term.setCursorPos(17, 1)
	term.setBackgroundColor(256)
	term.write(" Date   Block# Nonce               ")
	---------- (" Feb 28 000000 000000000000")
	term.setBackgroundColor(1)
	repeat
		ar = ar + 1
		term.setCursorPos(18, 1 + ar)
		term.write(blktime[ar])
		term.setCursorPos(31 - string.len(tostring(math.abs(tonumber(blkpeer[ar])))), 1 + ar)
		term.write(tonumber(blkpeer[ar]))
		term.setTextColor(32768)
		term.setCursorPos(32, 1 + ar)
		term.write(tonumber(blkhash[ar]))
	until ar == math.min(tx, 18)
elseif page == 13 then
	balance3 = tonumber(readURL(readconfig("syncnode") .. "?getbalance=" .. addresslv))
	term.setCursorPos(25, 2)
	term.write("Local vault manager")
	term.setCursorPos(19, 8)
	term.write("Local vaults are a place to put")
	term.setCursorPos(19, 9)
	term.write("Krist in the form of a file on")
	term.setCursorPos(19, 10)
	term.write("a computer. Unlike traditional")
	term.setCursorPos(19, 11)
	term.write("wallets, local vaults can only")
	term.setCursorPos(19, 12)
	term.write("be accessed on the computer")
	term.setCursorPos(19, 13)
	term.write("they were initially created on.")
	term.setCursorPos(19, 14)
	term.write("If you do this, please ensure")
	term.setCursorPos(19, 15)
	term.write("that this computer is never")
	term.setCursorPos(19, 16)
	term.write("stolen or broken, as your money")
	term.setCursorPos(19, 17)
	term.write("may be lost if you don't have a")
	term.setCursorPos(19, 18)
	term.write("backup.")
	term.setCursorPos(19, 4)
	term.write("KST put here: " .. balance3)
	term.setCursorPos(19, 5)
	term.write("Amount (KST): ")
	term.setCursorPos(33, 5)
	if amt == 0 then
		term.setTextColor(256)
		term.write("(click to set)")
	else
		term.setTextColor(32768)
		term.write(amt)
	end
	if tonumber(amt) > 0 then
		term.setCursorPos(25, 6)
		term.setTextColor(32768)
		term.setBackgroundColor(128)
		if tonumber(amt) <= balance then
			term.setBackgroundColor(2)
		end
		term.write(" Deposit ")
		term.setBackgroundColor(1)
		term.write(" ")
		term.setBackgroundColor(128)
		if tonumber(amt) <= balance3 then
			term.setBackgroundColor(2)
		end
		term.write(" Withdraw ")
		term.setBackgroundColor(1)
	end
elseif page == 14 then
	term.setBackgroundColor(1)
	term.setCursorPos(19, 2)
	term.write("Local settings")
	--deprecated for now
elseif page == 15 then
	term.setBackgroundColor(1)
	term.setCursorPos(18, 1)
	term.write(".KST domain name manager     [New]")
	term.setCursorPos(46, 1)
	term.setBackgroundColor(32)
	term.setTextColor(1)
	term.write(" + NEW")
	term.setCursorPos(17, 2)
	term.setBackgroundColor(256)
	term.setTextColor(32768)
	term.write(" Name                 Actions      ")
	term.setBackgroundColor(1)
	term.setCursorPos(18, 3)
	local namelist = readURL(readconfig("syncnode") .. "?listnames=" .. address)
	local splitname = split(namelist, ";")


	if #splitname == 0 then
		term.setTextColor(256)
		term.write("No names to display!")
	else
		local namecount = 1
		repeat
			local thisname = splitname[namecount]
			--namelist:sub(0,namelist:find(";")-1)
			term.setTextColor(32768)
			term.setCursorPos(18, 3 + namecount)
			term.write(splitname[namecount] .. ".kst")
			term.setCursorPos(39, 3 + namecount)
			term.setTextColor(512)
			if thisname == "a" or thisname == "name" or thisname == "owner" or thisname == "updated" or thisname == "registered" or thisname == "expires" or thisname == "id" or thisname == "unpaid" then term.setTextColor(256) end
			term.write("Edit Send ")
			term.setTextColor(256)
			term.write("Go")
			namecount = namecount + 1
		until namecount == #splitname + 1
	end
	--term.write("a.kst                Edit Send Go")
	term.setBackgroundColor(1)
elseif page == 16 then
	term.setBackgroundColor(1)
	term.setCursorPos(20, 2)
	term.write(".KST domain name registration")
	term.setCursorPos(19, 4)
	term.write("Name: ")
	if name == "" then
		term.setTextColor(colors.lightGray)
		term.write("(click to set)")
	else
		term.write(name)
		term.setTextColor(colors.lightGray)
		term.write(".kst")
	end
	term.setTextColor(colors.black)
	term.setCursorPos(19, 5)
	term.write("Cost: 500 KST")
	term.setCursorPos(19, 7)
	--term.write("Available! [Register]")
	if name == "" then
		term.setTextColor(colors.blue)
		term.write("Please select a name!")
	elseif availability == 1 then
		term.setTextColor(colors.green)
		term.write("Available! ")
		--if balance >= 500 then
		term.setBackgroundColor(colors.green)
		term.setTextColor(colors.lime)
		term.write(" Register ")
		term.setBackgroundColor(colors.white)
		--end
	elseif availability == 2 then
		term.setTextColor(colors.yellow)
		term.write("Name registered!")
	else
		term.setTextColor(colors.red)
		term.write("Not available!")
	end
	term.setTextColor(colors.black)
	term.setCursorPos(19, 9)
	term.write(".KST domain names are used on")
	term.setCursorPos(19, 10)
	term.write("the KristScape browser. For")
	term.setCursorPos(19, 11)
	term.write("more information, please see")
	term.setCursorPos(19, 12)
	term.write("the Krist thread.")
	term.setCursorPos(19, 14)
	term.write("All Krist spent on names will")
	term.setCursorPos(19, 15)
	term.write("be added to the value of")
	term.setCursorPos(19, 16)
	term.write("future blocks; essentially")
	term.setCursorPos(19, 17)
	term.write("being \"re-mined.\"")
elseif page == 17 then
	term.setBackgroundColor(1)
	term.setCursorPos(28, 2)
	term.write(".KST zone file")
	term.setCursorPos(19, 4)
	term.write("Name: " .. subject)
	term.setTextColor(colors.lightGray)
	term.write(".kst")
	term.setTextColor(colors.black)
	term.setCursorPos(19, 7)
	term.write("Your name's zone file is the")
	term.setCursorPos(19, 8)
	term.write("URL of the site it is pointing")
	term.setCursorPos(19, 9)
	term.write("to. When KristScape navigates")
	term.setCursorPos(19, 10)
	term.write("to a name, it will make an HTTP")
	term.setCursorPos(19, 11)
	term.write("get request to the above URL.")
	term.setCursorPos(19, 12)
	term.write("The zone record should not")
	term.setCursorPos(19, 13)
	term.write("include a protocol (http://)")
	term.setCursorPos(19, 14)
	term.write("and shouldn't end with a")
	term.setCursorPos(19, 15)
	term.write("slash. You can redirect a name")
	term.setCursorPos(19, 16)
	term.write("to another name by making the")
	term.setCursorPos(19, 17)
	term.write("first character of the record")
	term.setCursorPos(19, 18)
	term.write("a dollar sign; e.g. $krist.kst")
	term.setTextColor(colors.black)
	term.setCursorPos(19, 5)
	term.write("Zone: ")
	zone = readURL(readconfig("syncnode") .. "?a=" .. subject)
	if zone == "" then
		term.setTextColor(colors.lightGray)
		term.write("(click to set)")
	else
		term.write(zone)
	end
elseif page == 18 then
	term.setBackgroundColor(1)
	term.setCursorPos(28, 2)
	term.write("Name transfer")
	term.setCursorPos(19, 4)
	term.write("Name: " .. subject)
	term.setTextColor(colors.lightGray)
	term.write(".kst")
	term.setTextColor(colors.black)
	term.setCursorPos(19, 5)
	term.write("Recipient: ")
elseif page == 21 then
	term.setBackgroundColor(1)
	term.setCursorPos(4, 6)
	term.write("Address - ")
	term.setTextColor(16384)
	term.write(address)
	term.setTextColor(32768)
	term.setCursorPos(4, 7)
	term.write("Balance - ")
	term.setTextColor(1024)
	if tostring(balance) == 'nil' then balance = 0 end
	term.write(tostring(balance) .. " KST")
	term.setTextColor(32768)
	term.setCursorPos(3, 9)
end
