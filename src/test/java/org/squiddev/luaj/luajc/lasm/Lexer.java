package org.squiddev.luaj.luajc.lasm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Lexer {
	private static final HashSet<String> keywords = new HashSet<String>();
	private static final HashSet<Character> symbols = new HashSet<Character>();

	static {
		Collections.addAll(keywords,
			".const", ".local", ".name", ".options", ".local", ".upval", ".upvalue",
			".stacksize", ".maxstacksize", ".vararg", ".function", ".func", ".end",
			".params", ".args", ".arguments", ".argcount",
			"true", "false", "nil", "null"
		);

		Collections.addAll(symbols, '(', ')');
	}

	public enum TokenType {
		EOF,
		KEYWORD,
		IDENTIFIER,
		NUMBER,
		STRING,
		SYMBOL,
		LABEL,
	}


	public static final class Token {
		public final TokenType token;
		public final String contents;

		public int line;
		public int column;

		public Token(TokenType token, String contents) {
			this.token = token;
			this.contents = contents;
		}
	}

	public final String name;
	public final String contents;
	private int index;

	private int line = 1;
	private int column = 1;

	public Lexer(String name, String contents) {
		this.name = name == null ? "?" : name;
		this.contents = contents;
	}

	public char get() {
		if (index >= contents.length()) return '\0';

		char current = contents.charAt(index++);

		if (current == '\n') {
			line++;
			column = 1;
		} else {
			column++;
		}

		return current;
	}

	public char peek() {
		return index >= contents.length() ? '\0' : contents.charAt(index);
	}

	public void expect(char character) {
		if (peek() != character) throw lexError("Expected '" + character + "'");
		get();
	}

	public RuntimeException lexError(String message) {
		throw new RuntimeException(name + ":" + line + ":" + column + ": " + message);
	}

	public List<Token> lex() {
		ArrayList<Token> tokens = new ArrayList<Token>();
		while (true) {
			while (true) {
				char c = peek();
				if (c == ';') {
					get();
					while (c != '\0' && c != '\n') {
						get();
						c = peek();
					}
				} else if (c != '\0' && Character.isWhitespace(c)) {
					get();
				} else {
					break;
				}
			}

			int line = this.line, column = this.column;

			Token token;
			char c = peek();
			if (c == '\0') {
				token = new Token(TokenType.EOF, null);
			} else if (c == '_' || Character.isLetter(c)) {
				StringBuilder builder = new StringBuilder();
				do {
					builder.append(c);
					get();
					c = peek();
				} while (c == '_' || Character.isLetter(c));

				String word = builder.toString();
				token = new Token(keywords.contains(word) ? TokenType.KEYWORD : TokenType.IDENTIFIER, word);
			} else if (c == '-' || Character.isDigit(c)) {
				StringBuilder builder = new StringBuilder();
				do {
					builder.append(c);
					get();
					c = peek();
				} while (c == '.' || Character.isDigit(c));

				token = new Token(TokenType.NUMBER, builder.toString());
			} else if (c == '\'' || c == '"') {
				StringBuilder builder = new StringBuilder();
				char start = c;

				get();
				c = peek();
				do {
					get();
					builder.append(c);
					c = peek();
					if (c == '\\') {
						get();
						c = peek();
						switch (c) {
							case 'n':
								c = '\n';
								break;
							case 't':
								c = '\n';
								break;
							case 'r':
								c = '\r';
								break;
						}
					} else if (c == '\0') {
						throw lexError("Unfinished string");
					}
				} while (c != start);
				get();

				token = new Token(TokenType.STRING, builder.toString());
			} else if (c == '.') {
				StringBuilder builder = new StringBuilder();
				do {
					builder.append(c);
					get();
					c = peek();
				} while (c == '_' || Character.isLetter(c));

				String word = builder.toString();
				token = new Token(keywords.contains(word) ? TokenType.KEYWORD : TokenType.IDENTIFIER, word);
			} else if (c == ':') {
				get();
				expect(':');

				c = peek();
				StringBuilder builder = new StringBuilder();
				do {
					builder.append(c);
					get();
					c = peek();
				} while (c == '_' || Character.isLetter(c));

				expect(':');
				expect(':');

				token = new Token(TokenType.LABEL, builder.toString());
			} else if (symbols.contains(c)) {
				get();
				token = new Token(TokenType.SYMBOL, Character.toString(c));
			} else {
				throw lexError("Unknown character");
			}

			token.line = line;
			token.column = column;
			tokens.add(token);

			if (token.token == TokenType.EOF) break;
		}

		return tokens;
	}
}
