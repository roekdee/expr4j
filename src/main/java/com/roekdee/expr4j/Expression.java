package com.roekdee.expr4j;

import java.util.List;
import java.util.Map;

/**
 * Public facade for parsing and evaluating math expressions.
 *
 * <p>An {@code Expression} parses its source once on construction into an RPN
 * program, so it can be evaluated repeatedly with different variable bindings
 * without re-parsing.
 *
 * <pre>{@code
 * double a = Expression.evaluate("2 + 3 * 4");          // 14.0
 * Expression e = Expression.compile("x^2 + y");
 * double b = e.evaluate(Map.of("x", 3.0, "y", 1.0));    // 10.0
 * }</pre>
 */
public final class Expression {

    private final String source;
    private final List<RpnToken> rpn;

    private Expression(String source, List<RpnToken> rpn) {
        this.source = source;
        this.rpn = rpn;
    }

    /**
     * Parses {@code source} into a reusable, pre-compiled expression.
     *
     * @throws ExpressionException if the source is malformed
     */
    public static Expression compile(String source) {
        if (source == null || source.isBlank()) {
            throw new ExpressionException("Expression must not be empty");
        }
        List<Token> tokens = new Tokenizer(source).tokenize();
        if (tokens.isEmpty()) {
            throw new ExpressionException("Expression must not be empty");
        }
        List<RpnToken> rpn = new ShuntingYard(tokens).toRpn();
        return new Expression(source, rpn);
    }

    /** Evaluates this expression with no variables. */
    public double evaluate() {
        return evaluate(Map.of());
    }

    /** Evaluates this expression with the given variable bindings. */
    public double evaluate(Map<String, Double> variables) {
        return new Evaluator(rpn).evaluate(variables);
    }

    /** Convenience: parse and evaluate {@code source} with no variables. */
    public static double evaluate(String source) {
        return compile(source).evaluate();
    }

    /** Convenience: parse and evaluate {@code source} with variables. */
    public static double evaluate(String source, Map<String, Double> variables) {
        return compile(source).evaluate(variables);
    }

    /** Returns the original source string this expression was compiled from. */
    public String source() {
        return source;
    }

    @Override
    public String toString() {
        return "Expression[" + source + "]";
    }
}
