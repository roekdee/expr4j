package com.roekdee.expr4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Evaluates a Reverse Polish Notation stream produced by {@link ShuntingYard},
 * resolving variables against a binding map provided at evaluation time.
 */
public final class Evaluator {

    private final List<RpnToken> rpn;

    public Evaluator(List<RpnToken> rpn) {
        this.rpn = rpn;
    }

    /**
     * Evaluates the RPN program.
     *
     * @param variables variable bindings; may be empty but not {@code null}
     * @return the numeric result
     * @throws ExpressionException on unbound variables, arity mismatch, or
     *         malformed RPN (e.g. leftover operands)
     */
    public double evaluate(Map<String, Double> variables) {
        if (variables == null) {
            throw new ExpressionException("Variable map must not be null");
        }
        Deque<Double> stack = new ArrayDeque<>();
        for (RpnToken token : rpn) {
            if (token instanceof RpnToken.Number number) {
                stack.push(number.value());
            } else if (token instanceof RpnToken.Variable variable) {
                stack.push(resolveVariable(variable.name(), variables));
            } else if (token instanceof RpnToken.Op op) {
                applyOperator(op.operator(), stack);
            } else if (token instanceof RpnToken.Call call) {
                applyCall(call, stack);
            }
        }
        if (stack.size() != 1) {
            throw new ExpressionException(
                    "Malformed expression: " + stack.size() + " values left on the stack");
        }
        return stack.pop();
    }

    private double resolveVariable(String name, Map<String, Double> variables) {
        Double value = variables.get(name);
        if (value == null) {
            throw new ExpressionException("Unbound variable '" + name + "'");
        }
        return value;
    }

    private void applyOperator(Operator operator, Deque<Double> stack) {
        if (operator == Operator.NEGATE) {
            stack.push(-requireOne(stack, operator));
            return;
        }
        double right = requireOne(stack, operator);
        double left = requireOne(stack, operator);
        stack.push(switch (operator) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> divide(left, right);
            case MODULO -> modulo(left, right);
            case POWER -> Math.pow(left, right);
            case NEGATE -> throw new IllegalStateException("unreachable");
        });
    }

    private double divide(double left, double right) {
        if (right == 0.0) {
            throw new ExpressionException("Division by zero");
        }
        return left / right;
    }

    private double modulo(double left, double right) {
        if (right == 0.0) {
            throw new ExpressionException("Modulo by zero");
        }
        return left % right;
    }

    private void applyCall(RpnToken.Call call, Deque<Double> stack) {
        int argCount = call.argCount();
        if (stack.size() < argCount) {
            throw new ExpressionException(
                    "Function '" + call.name() + "' is missing arguments");
        }
        List<Double> args = new ArrayList<>(argCount);
        for (int i = 0; i < argCount; i++) {
            args.add(stack.pop());
        }
        java.util.Collections.reverse(args);
        stack.push(Functions.call(call.name(), args));
    }

    private double requireOne(Deque<Double> stack, Operator operator) {
        Double value = stack.poll();
        if (value == null) {
            throw new ExpressionException(
                    "Operator '" + operator.symbol() + "' is missing an operand");
        }
        return value;
    }
}
