package com.roekdee.expr4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Implements Dijkstra's shunting-yard algorithm, converting an infix token
 * stream into Reverse Polish Notation (RPN).
 *
 * <p>The algorithm extends the textbook version in three ways:
 * <ul>
 *   <li>It distinguishes unary minus from binary minus by tracking whether the
 *       previous token could end an operand.</li>
 *   <li>It supports function calls and counts their arguments using a parallel
 *       stack so that variadic functions ({@code min}, {@code max}) work.</li>
 *   <li>It validates parenthesis balance and reports the offending side.</li>
 * </ul>
 */
public final class ShuntingYard {

    private final List<Token> tokens;

    public ShuntingYard(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Converts the infix tokens to RPN.
     *
     * @return the RPN output queue, ready for {@link Evaluator}
     * @throws ExpressionException on malformed structure
     */
    public List<RpnToken> toRpn() {
        List<RpnToken> output = new ArrayList<>();
        Deque<StackItem> operators = new ArrayDeque<>();
        // One ArgFrame per active function-argument list (innermost on top).
        Deque<ArgFrame> argFrames = new ArrayDeque<>();

        boolean expectOperand = true; // true when the next token should start an operand

        for (Token token : tokens) {
            switch (token.type()) {
                case NUMBER -> {
                    output.add(new RpnToken.Number(parseNumber(token.text())));
                    markNonEmpty(argFrames);
                    expectOperand = false;
                }
                case IDENTIFIER -> {
                    if (Functions.isFunction(token.text())) {
                        operators.push(StackItem.function(token.text()));
                    } else {
                        output.add(new RpnToken.Variable(token.text()));
                        markNonEmpty(argFrames);
                        expectOperand = false;
                    }
                }
                case COMMA -> {
                    drainUntilLeftParen(operators, output, "Misplaced comma or unbalanced parentheses");
                    if (argFrames.isEmpty()) {
                        throw new ExpressionException("Comma used outside of a function call");
                    }
                    argFrames.peek().commaCount++;
                    expectOperand = true;
                }
                case OPERATOR -> {
                    if (token.text().equals("+") && expectOperand) {
                        // Unary plus is a no-op; consume it and keep expecting an operand.
                        break;
                    }
                    Operator op = resolveOperator(token.text(), expectOperand);
                    pushOperator(op, operators, output);
                    expectOperand = true;
                }
                case LEFT_PAREN -> {
                    boolean opensFunction = !operators.isEmpty() && operators.peek().isFunction();
                    if (opensFunction) {
                        operators.push(StackItem.leftParen());
                        argFrames.push(new ArgFrame());
                    } else {
                        // A grouping paren is itself an operand of any enclosing call.
                        markNonEmpty(argFrames);
                        operators.push(StackItem.leftParen());
                    }
                    expectOperand = true;
                }
                case RIGHT_PAREN -> {
                    drainUntilLeftParen(operators, output, "Unbalanced parentheses: extra ')'");
                    operators.pop(); // discard the left paren marker
                    if (!operators.isEmpty() && operators.peek().isFunction()) {
                        StackItem fn = operators.pop();
                        ArgFrame frame = argFrames.pop();
                        output.add(new RpnToken.Call(fn.functionName(), frame.argCount()));
                    }
                    expectOperand = false;
                }
            }
        }

        while (!operators.isEmpty()) {
            StackItem item = operators.pop();
            if (item.isParen()) {
                throw new ExpressionException("Unbalanced parentheses: missing ')'");
            }
            if (item.isFunction()) {
                throw new ExpressionException(
                        "Function '" + item.functionName() + "' must be followed by '('");
            }
            output.add(new RpnToken.Op(item.operator()));
        }
        return output;
    }

    private void markNonEmpty(Deque<ArgFrame> argFrames) {
        ArgFrame frame = argFrames.peek();
        if (frame != null) {
            frame.nonEmpty = true;
        }
    }

    private void pushOperator(Operator op, Deque<StackItem> operators, List<RpnToken> output) {
        while (!operators.isEmpty() && operators.peek().isOperator()) {
            Operator top = operators.peek().operator();
            boolean higher = top.precedence() > op.precedence();
            boolean equalLeft = top.precedence() == op.precedence() && op.isLeftAssociative();
            if (higher || equalLeft) {
                output.add(new RpnToken.Op(operators.pop().operator()));
            } else {
                break;
            }
        }
        operators.push(StackItem.operator(op));
    }

    private void drainUntilLeftParen(Deque<StackItem> operators, List<RpnToken> output, String onMissing) {
        while (!operators.isEmpty() && !operators.peek().isParen()) {
            StackItem item = operators.pop();
            if (item.isFunction()) {
                throw new ExpressionException(
                        "Function '" + item.functionName() + "' must be followed by '('");
            }
            output.add(new RpnToken.Op(item.operator()));
        }
        if (operators.isEmpty()) {
            throw new ExpressionException(onMissing);
        }
    }

    private Operator resolveOperator(String symbol, boolean expectOperand) {
        if (symbol.equals("-") && expectOperand) {
            return Operator.NEGATE;
        }
        Operator op = Operator.binaryFromSymbol(symbol);
        if (op == null) {
            throw new ExpressionException("Unknown operator '" + symbol + "'");
        }
        if (expectOperand) {
            throw new ExpressionException("Operator '" + symbol + "' has no left operand");
        }
        return op;
    }

    private double parseNumber(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new ExpressionException("Invalid number '" + text + "'", e);
        }
    }

    /** Tracks argument count for one active function-call argument list. */
    private static final class ArgFrame {
        private int commaCount;
        private boolean nonEmpty;

        int argCount() {
            if (!nonEmpty) {
                return 0;
            }
            return commaCount + 1;
        }
    }

    /** A tagged union of things that can sit on the operator stack. */
    private static final class StackItem {
        private enum Kind { OPERATOR, LEFT_PAREN, FUNCTION }

        private final Kind kind;
        private final Operator operator;
        private final String functionName;

        private StackItem(Kind kind, Operator operator, String functionName) {
            this.kind = kind;
            this.operator = operator;
            this.functionName = functionName;
        }

        static StackItem operator(Operator op) {
            return new StackItem(Kind.OPERATOR, op, null);
        }

        static StackItem leftParen() {
            return new StackItem(Kind.LEFT_PAREN, null, null);
        }

        static StackItem function(String name) {
            return new StackItem(Kind.FUNCTION, null, name);
        }

        boolean isOperator() {
            return kind == Kind.OPERATOR;
        }

        boolean isParen() {
            return kind == Kind.LEFT_PAREN;
        }

        boolean isFunction() {
            return kind == Kind.FUNCTION;
        }

        Operator operator() {
            return operator;
        }

        String functionName() {
            return functionName;
        }
    }
}
