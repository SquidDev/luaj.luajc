package org.squiddev.luaj.luajc.lasm;

import java.util.List;

/**
 * A stream for tokens.
 */
public final class TokenStream {
	public final String name;
	public final List<Lexer.Token> tokens;
	private int position = 0;

	public TokenStream(String filename, String contents) {
		this.name = filename;
		this.tokens = new Lexer(filename, contents).lex();
	}

	public Lexer.Token peek() {
		return position >= tokens.size() ? tokens.get(tokens.size() - 1) : tokens.get(position);
	}

	public Lexer.Token get() {
		return position >= tokens.size() ? tokens.get(tokens.size() - 1) : tokens.get(position++);
	}

	public boolean is(Lexer.TokenType type) {
		return peek().token == type;
	}

	public boolean consumeKeyword(String keyword) {
		Lexer.Token token = peek();
		if (token.token != Lexer.TokenType.KEYWORD || !token.contents.equals(keyword)) return false;

		get();
		return true;
	}

	public boolean consumeSymbol(String keyword) {
		Lexer.Token token = peek();
		if (token.token != Lexer.TokenType.SYMBOL || !token.contents.equals(keyword)) return false;

		get();
		return true;
	}

	public boolean consumeIdent(String keyword) {
		Lexer.Token token = peek();
		if (token.token != Lexer.TokenType.IDENTIFIER || !token.contents.equals(keyword)) return false;

		get();
		return true;
	}
}
