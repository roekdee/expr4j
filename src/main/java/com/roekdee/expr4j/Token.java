package com.roekdee.expr4j;

/**
 * A lexical token produced by the {@link Tokenizer}.
 *
 * @param type the token category
 * @param text the raw source text of the token
 */
public record Token(Type type, String text) {

    /** Token categories recognised by the grammar. */
    public enum Type {
        NUMBER,
        IDENTIFIER,
        OPERATOR,
        LEFT_PAREN,
        RIGHT_PAREN,
        COMMA
    }

    @Override
    public String toString() {
        return type + "(" + text + ")";
    }
}
