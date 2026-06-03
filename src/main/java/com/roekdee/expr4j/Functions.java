package com.roekdee.expr4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Built-in function registry. Each function takes an ordered list of
 * already-evaluated arguments and returns a double.
 */
public final class Functions {

    /** A named function with a fixed arity contract validated at call time. */
    @FunctionalInterface
    public interface Fn {
        double apply(List<Double> args);
    }

    private static final Map<String, Fn> REGISTRY = Map.of(
            "sqrt", unary("sqrt", Math::sqrt),
            "abs", unary("abs", Math::abs),
            "sin", unary("sin", Math::sin),
            "cos", unary("cos", Math::cos),
            "log", unary("log", Functions::log),
            "min", Functions::min,
            "max", Functions::max);

    private Functions() {
    }

    /** Returns {@code true} if {@code name} is a known built-in function. */
    public static boolean isFunction(String name) {
        return REGISTRY.containsKey(name);
    }

    /**
     * Invokes the named function.
     *
     * @throws ExpressionException if the function is unknown
     */
    public static double call(String name, List<Double> args) {
        Fn fn = REGISTRY.get(name);
        if (fn == null) {
            throw new ExpressionException("Unknown function '" + name + "'");
        }
        return fn.apply(args);
    }

    private static Fn unary(String name, Function<Double, Double> body) {
        return args -> {
            requireArity(name, args, 1);
            return body.apply(args.get(0));
        };
    }

    private static double log(double x) {
        if (x <= 0) {
            throw new ExpressionException("log requires a positive argument, got " + x);
        }
        return Math.log(x);
    }

    private static double min(List<Double> args) {
        requireAtLeast("min", args, 2);
        return args.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
    }

    private static double max(List<Double> args) {
        requireAtLeast("max", args, 2);
        return args.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
    }

    private static void requireArity(String name, List<Double> args, int expected) {
        if (args.size() != expected) {
            throw new ExpressionException(
                    name + " expects " + expected + " argument(s) but got " + args.size());
        }
    }

    private static void requireAtLeast(String name, List<Double> args, int min) {
        if (args.size() < min) {
            throw new ExpressionException(
                    name + " expects at least " + min + " arguments but got " + args.size());
        }
    }
}
