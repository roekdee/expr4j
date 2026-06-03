package com.roekdee.expr4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts an expression string into a flat list of {@link Token}s.
 *
 * <p>The tokenizer is intentionally simple and context-free: it does not try
 * to distinguish unary from binary minus (that is resolved later in
 * {@link ShuntingYard}). It recognises numbers (including decimals and
 * scientific notation), identifiers (variables and function names),
 * single-character operators, parentheses, and argument commas.
 */
public final class Tokenizer {

    private final String input;
    private int pos;

    public Tokenizer(String input) {
        if (input == null) {
            throw new ExpressionException("Expression must not be null");
        }
        this.input = input;
    }

    /**
     * Tokenizes the whole input.
     *
     * @return the ordered list of tokens
     * @throws ExpressionException if an unexpected character is encountered
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (isNumberStart(c)) {
                tokens.add(readNumber());
            } else if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifier());
            } else if (isOperator(c)) {
                tokens.add(new Token(Token.Type.OPERATOR, String.valueOf(c)));
                pos++;
            } else if (c == '(') {
                tokens.add(new Token(Token.Type.LEFT_PAREN, "("));
                pos++;
            } else if (c == ')') {
                tokens.add(new Token(Token.Type.RIGHT_PAREN, ")"));
                pos++;
            } else if (c == ',') {
                tokens.add(new Token(Token.Type.COMMA, ","));
                pos++;
            } else {
                throw new ExpressionException(
                        "Unknown token '" + c + "' at position " + pos);
            }
        }
        return tokens;
    }

    private boolean isNumberStart(char c) {
        return Character.isDigit(c) || c == '.';
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '^';
    }

    private Token readNumber() {
        int start = pos;
        boolean seenDot = false;
        boolean seenExp = false;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' && !seenDot && !seenExp) {
                seenDot = true;
                pos++;
            } else if ((c == 'e' || c == 'E') && !seenExp && pos > start) {
                seenExp = true;
                pos++;
                if (pos < input.length()
                        && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                    pos++;
                }
            } else {
                break;
            }
        }
        String text = input.substring(start, pos);
        if (text.equals(".")) {
            throw new ExpressionException("Malformed number at position " + start);
        }
        return new Token(Token.Type.NUMBER, text);
    }

    private Token readIdentifier() {
        int start = pos;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') {
                pos++;
            } else {
                break;
            }
        }
        return new Token(Token.Type.IDENTIFIER, input.substring(start, pos));
    }
}
