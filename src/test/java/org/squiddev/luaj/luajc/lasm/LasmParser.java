package org.squiddev.luaj.luajc.lasm;

import org.luaj.vm2.*;
import org.squiddev.luaj.luajc.utils.DumpInstructions;
import org.squiddev.luaj.luajc.utils.IntArray;

import java.util.*;

import static org.squiddev.luaj.luajc.lasm.Lexer.Token;
import static org.squiddev.luaj.luajc.lasm.Lexer.TokenType;
import static org.squiddev.luaj.luajc.utils.DumpInstructions.ArgType;

public final class LasmParser {

	private final TokenStream stream;
	private final Deque<State> states = new ArrayDeque<State>();
	private State state;

	public LasmParser(TokenStream stream) {
		this.stream = stream;
		state = new State();
		state.proto.source = LuaValue.valueOf("?");
	}

	public LasmParser(String name, String contents) {
		this(new TokenStream(name, contents));
		state.proto.source = LuaValue.valueOf(name);
	}

	private final class Jump {
		public final int pc;
		public final String label;

		private Jump(int pc, String label) {
			this.pc = pc;
			this.label = label;
		}
	}

	private class State {
		private final List<Jump> jumps = new ArrayList<Jump>();
		private final Map<String, Integer> labels = new HashMap<String, Integer>();

		private final IntArray code = new IntArray();
		private final List<LuaString> upvalues = new ArrayList<LuaString>();
		private final List<Prototype> prototypes = new ArrayList<Prototype>();

		private final List<LuaValue> constants = new ArrayList<LuaValue>();
		private final Map<LuaValue, Integer> constantMap = new HashMap<LuaValue, Integer>();

		private final Prototype proto = new Prototype();

		private void finish() {
			for (Jump jump : jumps) {
				Integer target = labels.get(jump.label);
				if (target == null) throw new RuntimeException("No such label " + jump.label);

				System.out.println("Jump from " + jump.pc + " to " + target);

				code.set(jump.pc, LuaC.SETARG_sBx(code.get(jump.pc), target - jump.pc - 1));
			}

			code.add(LuaC.CREATE_ABC(Lua.OP_RETURN, 0, 1, 0));

			proto.nups = upvalues.size();
			proto.upvalues = upvalues.toArray(new LuaString[upvalues.size()]);

			proto.k = constants.toArray(new LuaValue[constants.size()]);
			proto.code = code.toArray();

			proto.p = prototypes.toArray(new Prototype[prototypes.size()]);

			proto.locvars = new LocVars[0];
			proto.lineinfo = new int[code.size()];
		}

		private int getConstant(LuaValue value) {
			Integer index = constantMap.get(value);
			if (index != null) return index;

			int idx = constants.size();
			constants.add(value);
			constantMap.put(value, idx);

			return idx;
		}

	}

	private RuntimeException parseError(String message) {
		Token token = stream.get();
		return new RuntimeException(stream.name + ":" + token.line + ":" + token.column + ": " + message + " (got " + token.token.toString().toLowerCase() + ")");
	}

	private int addConstant() {
		Token token = stream.peek();
		switch (token.token) {
			case STRING:
				stream.get();
				return state.getConstant(LuaValue.valueOf(token.contents));
			case NUMBER:
				stream.get();
				return state.getConstant(LuaValue.valueOf(Double.parseDouble(token.contents)));
			case KEYWORD:
				if (token.contents.equals("nil")) {
					stream.get();
					return state.getConstant(LuaValue.NIL);
				} else if (token.contents.equals("true")) {
					stream.get();
					return state.getConstant(LuaValue.TRUE);
				} else if (token.contents.equals("false")) {
					stream.get();
					return state.getConstant(LuaValue.FALSE);
				}
				break;
		}

		throw parseError("Expected constant");
	}

	private int manageConstant(boolean paren) {
		if (stream.consumeSymbol("(")) {
			paren = true;
		} else if (paren) {
			throw parseError("Expected (");
		}

		int index = addConstant();

		if (paren && !stream.consumeSymbol(")")) {
			throw parseError("Expected )");
		}

		return index;
	}

	private void doControl() {
		if (stream.consumeKeyword(".const")) {
			addConstant();
		} else if (stream.consumeKeyword(".name")) {
			if (stream.is(TokenType.STRING) || stream.is(TokenType.IDENTIFIER)) {
				state.proto.source = LuaValue.valueOf(stream.get().contents);
			} else {
				throw parseError("Expected string or identifier");
			}
		} else if (stream.consumeKeyword(".params") || stream.consumeKeyword(".args")) {
			if (stream.is(TokenType.NUMBER)) {
				state.proto.numparams = Integer.parseInt(stream.get().contents);
			} else {
				throw parseError("Expected number");
			}
		} else if (stream.consumeKeyword(".maxstack") || stream.consumeKeyword(".stack")) {
			if (stream.is(TokenType.NUMBER)) {
				state.proto.maxstacksize = Integer.parseInt(stream.get().contents);
			} else {
				throw parseError("Expected number");
			}
		} else if (stream.consumeKeyword(".varargs")) {
			state.proto.is_vararg = Lua.VARARG_ISVARARG;
		} else if (stream.consumeKeyword(".upvalue")) {
			if (stream.is(TokenType.STRING) || stream.is(TokenType.IDENTIFIER)) {
				state.upvalues.add(LuaValue.valueOf(stream.get().contents));
			} else {
				throw parseError("Expected string or identifier");
			}
		} else if (stream.consumeKeyword(".func") || stream.consumeKeyword(".function")) {
			State next = new State();
			next.proto.source = state.proto.source;
			state.prototypes.add(next.proto);
			states.push(state);
			state = next;

			if (stream.is(TokenType.STRING)) next.proto.source = LuaString.valueOf(stream.get().contents);
		} else if (stream.consumeKeyword(".end")) {
			state.finish();
			state = states.pollFirst();
		}
	}

	private int parseNumber(boolean isRK) {
		int offset = isRK ? 256 : 0;
		if (stream.consumeIdent("k") || stream.consumeIdent("const")) {
			return offset + manageConstant(true);
		} else if (stream.consumeKeyword("true")) {
			return offset + state.getConstant(LuaValue.TRUE);
		} else if (stream.consumeKeyword("false")) {
			return offset + state.getConstant(LuaValue.TRUE);
		} else if (stream.is(TokenType.NUMBER)) {
			return offset + Integer.parseInt(stream.get().contents);
		} else {
			throw parseError("Expected number or constant");
		}
	}

	private int parseArg(int pc, ArgType type) {
		switch (type) {
			case Unused:
				return 0;
			case Constant:
				if (stream.is(TokenType.STRING)) {
					return addConstant();
				} else {
					return parseNumber(false);
				}
			case ConstantOrRegister: {
				int idx = parseNumber(true);
				if (idx <= 255 && idx >= state.proto.maxstacksize) state.proto.maxstacksize = idx + 1;
				return idx;
			}
			case Closure:
				if (stream.is(TokenType.NUMBER)) {
					int idx = Integer.parseInt(stream.get().contents);
					if (idx >= state.proto.maxstacksize) state.proto.maxstacksize = idx + 1;
					return idx;
				} else if (stream.is(TokenType.STRING)) {
					LuaValue value = LuaValue.valueOf(stream.peek().contents);
					int i = 0;
					for (Prototype proto : state.prototypes) {
						if (proto.source.equals(value)) {
							stream.get();
							return i;
						}

						i++;
					}

					throw parseError("Unknown prototype");
				} else {
					throw parseError("Expected " + type);
				}
			case General:
			case Upvalue:
				if (stream.is(TokenType.NUMBER)) {
					return Integer.parseInt(stream.get().contents);
				} else {
					throw parseError("Expected " + type);
				}
			case Register:
				if (stream.is(TokenType.NUMBER)) {
					int idx = Integer.parseInt(stream.get().contents);
					if (idx >= state.proto.maxstacksize) state.proto.maxstacksize = idx + 1;
					return idx;
				} else {
					throw parseError("Expected " + type);
				}
			case JumpDistance:
				if (stream.is(TokenType.LABEL) || stream.is(TokenType.IDENTIFIER)) {
					String name = stream.get().contents;
					state.jumps.add(new Jump(pc, name));

					return 0;
				} else if (stream.is(Lexer.TokenType.NUMBER)) {
					return Integer.parseInt(stream.get().contents);
				} else {
					throw parseError("Expected " + type);
				}
			default:
				throw new RuntimeException("Unknown type " + type);

		}
	}

	private int parseOpcode(int pc) {
		if (!stream.is(Lexer.TokenType.IDENTIFIER)) throw parseError("Opcode expected");

		String token = stream.get().contents;
		Integer index = DumpInstructions.OPCODES.get(token.toUpperCase());
		if (index == null) throw parseError("Unknown opcode " + token);

		int opcode = index;
		ArgType[] types = DumpInstructions.ARG_TYPES[opcode];

		switch (Lua.getOpMode(opcode)) {
			case Lua.iABC: {
				return LuaC.CREATE_ABC(opcode, parseArg(pc, types[0]), parseArg(pc, types[1]), parseArg(pc, types[2]));
			}
			case Lua.iABx:
				return LuaC.CREATE_ABx(opcode, parseArg(pc, types[0]), parseArg(pc, types[1]));
			case Lua.iAsBx:
				return LuaC.CREATE_AsBx(opcode, parseArg(pc, types[0]), parseArg(pc, types[1]));
			default:
				throw new RuntimeException("Unknown type " + Lua.getOpMode(opcode));
		}
	}

	public Prototype parse() {
		Prototype proto = state.proto;
		int pc = 0;
		while (!stream.is(TokenType.EOF)) {
			if (state == null) {
				throw parseError("Unexpected end");
			}

			if (stream.is(TokenType.KEYWORD) && stream.peek().contents.charAt(0) == '.') {
				doControl();
			} else if (stream.is(TokenType.IDENTIFIER) && stream.peek().contents.charAt(0) == '.') {
				throw parseError("Unknown control");
			} else if (stream.is(TokenType.LABEL)) {
				state.labels.put(stream.get().contents, pc);
			} else {
				state.code.add(parseOpcode(pc));
				pc++;
			}
		}

		if (state != null) {
			state.finish();
		}

		return proto;
	}
}
