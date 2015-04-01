package org.squiddev.luaj.luajc;

import org.luaj.vm2.*;

import java.io.PrintStream;

/**
 * Dumps the contents of a Lua prototype to a readable format
 */
public class DumpInstructions {
	protected static final String[] OPCODE_NAMES = {
		"MOVE", "LOADK", "LOADBOOL", "LOADNIL", "GETUPVAL", "GETGLOBAL",
		"GETTABLE", "SETGLOBAL", "SETUPVAL", "SETTABLE", "NEWTABLE", "SELF",
		"ADD", "SUB", "MUL", "DIV", "MOD", "POW", "UNM", "NOT", "LEN", "CONCAT",
		"JMP", "EQ", "LT", "LE", "TEST", "TESTSET", "CALL", "TAILCALL", "RETURN",
		"FORLOOP", "FORPREP", "TFORLOOP", "SETLIST", "CLOSE", "CLOSURE", "VARARG",
	};

	public enum ArgType {
		Unused,
		General,
		Register,
		Constant,
		ConstantOrRegister,
		Upvalue,
		JumpDistance,
	}

	protected static final ArgType[][] ARG_TYPES = {
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.Unused}, // OP_MOVE
		new ArgType[]{ArgType.Register, ArgType.Constant, ArgType.Unused}, // OP_LOADK
		new ArgType[]{ArgType.Register, ArgType.General, ArgType.General}, // OP_LOADBOOL
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.Register}, // OP_LOADNIL
		new ArgType[]{ArgType.Register, ArgType.Upvalue, ArgType.Unused}, // OP_GETUPVAL
		new ArgType[]{ArgType.Register, ArgType.Constant, ArgType.Unused}, // OP_GETGLOBAL
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.ConstantOrRegister}, // OP_GETTABLE
		new ArgType[]{ArgType.Register, ArgType.Constant, ArgType.Unused}, // OP_SETGLOBAL
		new ArgType[]{ArgType.Register, ArgType.Upvalue, ArgType.Unused}, // OP_SETUPVAL
		new ArgType[]{ArgType.Register, ArgType.ConstantOrRegister, ArgType.ConstantOrRegister}, // OP_SETTABLE
		new ArgType[]{ArgType.Register, ArgType.General, ArgType.General}, // OP_NEWTABLE
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.ConstantOrRegister}, // OP_SELF
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.ConstantOrRegister}, // OP_ADD
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.ConstantOrRegister}, // OP_SUB
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.ConstantOrRegister}, // OP_MUL
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.ConstantOrRegister}, // OP_DIV
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.ConstantOrRegister}, // OP_MOD
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.ConstantOrRegister}, // OP_POW
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.Unused}, // OP_UNM
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.Unused}, // OP_NOT
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.Unused}, // OP_LEN
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.Register}, // OP_CONCAT
		new ArgType[]{ArgType.Unused, ArgType.JumpDistance, ArgType.Unused}, // OP_JMP
		new ArgType[]{ArgType.Register, ArgType.ConstantOrRegister, ArgType.ConstantOrRegister}, // OP_EQ
		new ArgType[]{ArgType.Register, ArgType.ConstantOrRegister, ArgType.ConstantOrRegister}, // OP_LT
		new ArgType[]{ArgType.Register, ArgType.ConstantOrRegister, ArgType.ConstantOrRegister}, // OP_LE
		new ArgType[]{ArgType.Register, ArgType.Unused, ArgType.Register}, // OP_TEST
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.Register}, // OP_TESTSET
		new ArgType[]{ArgType.Register, ArgType.General, ArgType.General}, // OP_CALL
		new ArgType[]{ArgType.Register, ArgType.General, ArgType.General}, // OP_TAILCALL
		new ArgType[]{ArgType.Register, ArgType.General, ArgType.Unused}, // OP_RETURN
		new ArgType[]{ArgType.Register, ArgType.JumpDistance, ArgType.Unused}, // OP_FORLOOP
		new ArgType[]{ArgType.Register, ArgType.JumpDistance, ArgType.Unused}, // OP_FORPREP
		new ArgType[]{ArgType.Register, ArgType.JumpDistance, ArgType.General}, // OP_TFORLOOP
		new ArgType[]{ArgType.Register, ArgType.General, ArgType.General}, // OP_SETLIST
		new ArgType[]{ArgType.Register, ArgType.Unused, ArgType.Unused}, // OP_CLOSE
		new ArgType[]{ArgType.Register, ArgType.General, ArgType.Unused}, // OP_CLOSURE
		new ArgType[]{ArgType.Register, ArgType.Register, ArgType.Unused}, // OP_VARARG
	};

	protected String indent = "";
	protected final PrintStream stream;

	public DumpInstructions(Prototype proto, PrintStream stream) {
		this.stream = stream;

		stream.println("; Main Function");
		dumpFunction(proto);
	}

	protected void doIntent() {
		indent += "\t";
	}

	protected void unIndent() {
		indent = indent.substring(1);
	}

	protected void write(String text) {
		stream.print(indent);
		stream.println(text.trim());
	}

	protected void write() {
		stream.println();
	}

	protected void dumpFunction(Prototype chunk) {
		write(".name \"" + chunk.source + "\"");
		write(".options " + chunk.nups + " " + chunk.numparams + " " + chunk.is_vararg + " " + chunk.maxstacksize);
		write("; Above contains: Upvalue count, Argument count, Vararg flag, Max Stack Size");
		write();

		if (chunk.k.length > 0) {
			write("; Constants");
			for (LuaValue k : chunk.k) {
				write(".const " + formatConstant(k));
			}

			write();
		}

		if (chunk.locvars.length > 0) {
			write("; Locals");
			for (LocVars local : chunk.locvars) {
				write(".local '" + local.varname.toString() + "' ; " + local.startpc + " to " + local.endpc);
			}
			write();
		}

		if (chunk.upvalues.length > 0) {
			write("; Upvalues");
			for (LuaString upvalue : chunk.upvalues) {
				write(".upvalue '" + upvalue + "'");
			}
			write();
		}

		write("; Instructions");
		int previousLine = -1;
		int pc = 0;

		for (int instr : chunk.code) {
			int line = chunk.lineinfo[pc];
			if (line != previousLine) {
				if (pc != 0) write();
				write("; Line: " + line + ", PC: " + pc);
				previousLine = line;
			}

			int opcode = Lua.GET_OPCODE(instr);
			StringBuilder builder = new StringBuilder(OPCODE_NAMES[opcode].toLowerCase())
				.append(" ").append(formatArg(Lua.GETARG_A(instr), ARG_TYPES[opcode][0], chunk)).append(" ");
			switch (Lua.getOpMode(opcode)) {
				case Lua.iABC:
					builder
						.append(formatArg(Lua.GETARG_B(instr), ARG_TYPES[opcode][1], chunk)).append(" ")
						.append(formatArg(Lua.GETARG_C(instr), ARG_TYPES[opcode][2], chunk));
					break;
				case Lua.iABx:
					builder
						.append(formatArg(Lua.GETARG_Bx(instr), ARG_TYPES[opcode][1], chunk));
					break;
				case Lua.iAsBx:
					builder
						.append(formatArg(Lua.GETARG_sBx(instr), ARG_TYPES[opcode][1], chunk));
					break;
			}

			write(builder.toString());

			++pc;
		}

		if (chunk.p.length > 0) {
			write("; Protos");
			for (Prototype proto : chunk.p) {
				write();
				write("; Function " + proto.source);
				write(".func");
				doIntent();
				dumpFunction(proto);
				unIndent();
				write(".end");
			}
			write();
		}
	}

	protected String formatArg(int arg, ArgType type, Prototype proto) {
		if (type == ArgType.Constant) {
			if (arg >= 256) arg -= 256;
			return formatConstant(proto.k[arg]);
		} else if (type == ArgType.ConstantOrRegister && arg >= 256) {
			return formatConstant(proto.k[arg - 256]);
		} else if (type == ArgType.Upvalue) {
			return "up-" + arg + "";
		} else if (type == ArgType.JumpDistance) {
			return "+" + arg;
		} else if (type == ArgType.Unused) {
			return "";
		}

		return Integer.toString(arg);
	}

	protected String formatConstant(LuaValue k) {
		if (k instanceof LuaNil || k instanceof LuaBoolean || k instanceof LuaNumber) {
			return k.toString();
		} else if (k instanceof LuaString) {
			StringBuilder builder = new StringBuilder().append('"');

			LuaString s = (LuaString) k;
			int length = s.length();
			for (int i = 0; i < length; i++) {
				int c = s.luaByte(i);
				if (c < 32 || c == '\\' || c == '"' || c > 126) {
					if (c >= 7 && c <= 13) {
						builder.append("\\").append("abtnvfr".charAt(c - 7));
					} else if (c == '\\' || c == '"') {
						builder.append("\\").append((char) c);
					} else {
						builder.append("\\").append(c);
					}
				} else {
					builder.append((char) c);
				}
			}

			return builder.append('"').toString();
		}

		throw new RuntimeException("Unsupported " + k.typename());
	}

	public static void dump(Prototype proto, PrintStream writer) {
		new DumpInstructions(proto, writer);
	}

	public static void dump(Prototype proto) {
		dump(proto, System.out);
	}
}
