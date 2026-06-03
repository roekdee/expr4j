package com.roekdee.expr4j;

/**
 * Describes a binary or unary operator: its symbol, precedence, and
 * associativity. Higher precedence binds tighter.
 */
public enum Operator {

    ADD("+", 2, Associativity.LEFT),
    SUBTRACT("-", 2, Associativity.LEFT),
    MULTIPLY("*", 3, Associativity.LEFT),
    DIVIDE("/", 3, Associativity.LEFT),
    MODULO("%", 3, Associativity.LEFT),
    POWER("^", 4, Associativity.RIGHT),
    /** Unary minus. Right-associative and higher precedence than power. */
    NEGATE("u-", 5, Associativity.RIGHT);

    /** Operator associativity. */
    public enum Associativity {
        LEFT,
        RIGHT
    }

    private final String symbol;
    private final int precedence;
    private final Associativity associativity;

    Operator(String symbol, int precedence, Associativity associativity) {
        this.symbol = symbol;
        this.precedence = precedence;
        this.associativity = associativity;
    }

    public String symbol() {
        return symbol;
    }

    public int precedence() {
        return precedence;
    }

    public Associativity associativity() {
        return associativity;
    }

    public boolean isLeftAssociative() {
        return associativity == Associativity.LEFT;
    }

    /** Returns the binary operator for the given symbol, or {@code null}. */
    public static Operator binaryFromSymbol(String symbol) {
        return switch (symbol) {
            case "+" -> ADD;
            case "-" -> SUBTRACT;
            case "*" -> MULTIPLY;
            case "/" -> DIVIDE;
            case "%" -> MODULO;
            case "^" -> POWER;
            default -> null;
        };
    }
}
