package com.roekdee.expr4j;

/**
 * Thrown when an expression cannot be tokenized, parsed, or evaluated.
 *
 * <p>Typical causes include unbalanced parentheses, unknown tokens, an
 * unbound variable, an unknown function, a wrong argument count, or
 * division by zero.
 */
public class ExpressionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExpressionException(String message) {
        super(message);
    }

    public ExpressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
