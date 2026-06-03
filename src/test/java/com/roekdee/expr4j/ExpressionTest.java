package com.roekdee.expr4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionTest {

    private static final double EPS = 1e-9;

    @Nested
    @DisplayName("Operator precedence and associativity")
    class PrecedenceAndAssociativity {

        @ParameterizedTest
        @CsvSource({
                "2+3*4, 14",
                "2*3+4, 10",
                "10-2-3, 5",          // subtraction is left-associative
                "100/10/2, 5",        // division is left-associative
                "2+3*4-1, 13",
                "(2+3)*4, 20",
                "2*(3+4), 14",
                "10%3, 1",
                "10%3+1, 2"
        })
        void evaluatesArithmetic(String expr, double expected) {
            assertEquals(expected, Expression.evaluate(expr), EPS);
        }

        @Test
        @DisplayName("^ is right-associative")
        void powerIsRightAssociative() {
            // 2^3^2 == 2^(3^2) == 2^9 == 512, not (2^3)^2 == 64
            assertEquals(512.0, Expression.evaluate("2^3^2"), EPS);
        }

        @Test
        @DisplayName("^ binds tighter than * and +")
        void powerBindsTighter() {
            assertEquals(2.0 * 8.0, Expression.evaluate("2*2^3"), EPS);
            assertEquals(1.0 + 8.0, Expression.evaluate("1+2^3"), EPS);
        }
    }

    @Nested
    @DisplayName("Unary minus")
    class UnaryMinus {

        @ParameterizedTest
        @CsvSource({
                "-5, -5",
                "-5+3, -2",
                "3+-5, -2",
                "-(2+3), -5",
                "2--3, 5",            // 2 - (-3)
                "-2*3, -6"
        })
        void handlesUnaryMinus(String expr, double expected) {
            assertEquals(expected, Expression.evaluate(expr), EPS);
        }

        @Test
        @DisplayName("-2^2 evaluates as -(2^2) is not assumed; negate binds tighter")
        void negateBindsTighterThanPower() {
            // With NEGATE precedence above POWER, -2^2 == (-2)^2 == 4
            assertEquals(4.0, Expression.evaluate("-2^2"), EPS);
        }

        @Test
        @DisplayName("unary plus is a no-op")
        void unaryPlusIsNoOp() {
            assertEquals(5.0, Expression.evaluate("+5"), EPS);
            assertEquals(2.0, Expression.evaluate("+2*+1"), EPS);
        }
    }

    @Nested
    @DisplayName("Functions")
    class FunctionTests {

        @Test
        void sqrtAbs() {
            assertEquals(3.0, Expression.evaluate("sqrt(9)"), EPS);
            assertEquals(5.0, Expression.evaluate("abs(-5)"), EPS);
            assertEquals(5.0, Expression.evaluate("abs(0-5)"), EPS);
        }

        @Test
        void minMaxAreVariadic() {
            assertEquals(1.0, Expression.evaluate("min(1, 2, 3)"), EPS);
            assertEquals(3.0, Expression.evaluate("max(1, 2, 3)"), EPS);
            assertEquals(2.0, Expression.evaluate("min(5, 2)"), EPS);
        }

        @Test
        void trigonometryAndLog() {
            assertEquals(0.0, Expression.evaluate("sin(0)"), EPS);
            assertEquals(1.0, Expression.evaluate("cos(0)"), EPS);
            assertEquals(0.0, Expression.evaluate("log(1)"), EPS);
        }

        @Test
        void nestedFunctionsAndExpressions() {
            // max(sqrt(16), 2*1.5) = max(4, 3) = 4
            assertEquals(4.0, Expression.evaluate("max(sqrt(16), 2*1.5)"), EPS);
            // sqrt(abs(-16)) = 4
            assertEquals(4.0, Expression.evaluate("sqrt(abs(-16))"), EPS);
        }
    }

    @Nested
    @DisplayName("Variables")
    class Variables {

        @Test
        void boundAtEvaluationTime() {
            Expression e = Expression.compile("x^2 + y");
            assertEquals(10.0, e.evaluate(Map.of("x", 3.0, "y", 1.0)), EPS);
            // Re-evaluate with different bindings, no re-parse.
            assertEquals(5.0, e.evaluate(Map.of("x", 2.0, "y", 1.0)), EPS);
        }

        @Test
        void variablesInsideFunctions() {
            assertEquals(5.0,
                    Expression.evaluate("max(x, y)", Map.of("x", 5.0, "y", 3.0)), EPS);
        }

        @Test
        void scientificNotationNumbers() {
            assertEquals(1500.0, Expression.evaluate("1.5e3"), EPS);
            assertEquals(0.0125, Expression.evaluate("1.25e-2"), EPS);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        void unbalancedParentheses() {
            assertThrows(ExpressionException.class, () -> Expression.evaluate("(1+2"));
            assertThrows(ExpressionException.class, () -> Expression.evaluate("1+2)"));
            assertThrows(ExpressionException.class, () -> Expression.evaluate("((1)"));
        }

        @Test
        void divisionByZero() {
            assertThrows(ExpressionException.class, () -> Expression.evaluate("1/0"));
            assertThrows(ExpressionException.class, () -> Expression.evaluate("5%0"));
        }

        @Test
        void unknownToken() {
            assertThrows(ExpressionException.class, () -> Expression.evaluate("2 @ 3"));
        }

        @Test
        void unknownFunctionIsTreatedAsVariableThenFailsBinding() {
            // "foo" is not a known function, so it is parsed as a variable and
            // fails because no binding was supplied.
            assertThrows(ExpressionException.class, () -> Expression.evaluate("foo + 1"));
        }

        @Test
        void unboundVariable() {
            ExpressionException ex = assertThrows(ExpressionException.class,
                    () -> Expression.evaluate("x + 1"));
            org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("x"));
        }

        @Test
        void emptyExpression() {
            assertThrows(ExpressionException.class, () -> Expression.evaluate(""));
            assertThrows(ExpressionException.class, () -> Expression.evaluate("   "));
        }

        @Test
        void danglingOperator() {
            assertThrows(ExpressionException.class, () -> Expression.evaluate("2+"));
            assertThrows(ExpressionException.class, () -> Expression.evaluate("*2"));
        }

        @Test
        void wrongFunctionArity() {
            assertThrows(ExpressionException.class, () -> Expression.evaluate("sqrt(1, 2)"));
            assertThrows(ExpressionException.class, () -> Expression.evaluate("min(1)"));
        }

        @Test
        void nullVariableMapRejected() {
            Expression e = Expression.compile("1+1");
            assertThrows(ExpressionException.class, () -> e.evaluate(null));
        }
    }
}
