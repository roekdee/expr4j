package com.roekdee.expr4j;

/**
 * A token in the Reverse Polish Notation (RPN) output of {@link ShuntingYard}.
 *
 * <p>Unlike the lexer {@link Token}, an RPN token carries semantic intent:
 * numbers, variable references, resolved operators, and function calls with a
 * concrete argument count.
 */
public sealed interface RpnToken
        permits RpnToken.Number, RpnToken.Variable, RpnToken.Op, RpnToken.Call {

    /** A numeric literal. */
    record Number(double value) implements RpnToken {
    }

    /** A variable reference resolved against the binding map at eval time. */
    record Variable(String name) implements RpnToken {
    }

    /** A resolved operator (binary or unary). */
    record Op(Operator operator) implements RpnToken {
    }

    /** A function invocation with the number of arguments collected for it. */
    record Call(String name, int argCount) implements RpnToken {
    }
}
