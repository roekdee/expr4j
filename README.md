# expr4j

![CI](https://github.com/roekdee/expr4j/actions/workflows/ci.yml/badge.svg)

A small Java library that parses and evaluates math expressions. It uses the shunting-yard algorithm to turn infix text into RPN, then runs that through a stack machine. No dependencies beyond JUnit for the tests.

It handles `+ - * / %` and `^` (right-associative, so `2^3^2` is 512), unary minus, parentheses, named variables, and a handful of built-in functions (`sqrt`, `abs`, `min`, `max`, `sin`, `cos`, `log` — `min` and `max` take any number of args). Numbers can be decimal or scientific notation like `1.25e-2`.

## Usage

```java
import com.roekdee.expr4j.Expression;
import java.util.Map;

double a = Expression.evaluate("2 + 3 * 4");      // 14.0
double b = Expression.evaluate("sqrt(abs(-16))"); // 4.0

// Compile once, evaluate many times with different variables
Expression poly = Expression.compile("x^2 + y");
double c = poly.evaluate(Map.of("x", 3.0, "y", 1.0)); // 10.0
double d = poly.evaluate(Map.of("x", 2.0, "y", 1.0)); // 5.0
```

Bad input throws `ExpressionException` with a message that says what went wrong:

```java
Expression.evaluate("(1 + 2");  // Unbalanced parentheses: missing ')'
Expression.evaluate("1 / 0");   // Division by zero
Expression.evaluate("x + 1");   // Unbound variable 'x'
```

## Build & test

```bash
mvn test
```

Needs JDK 17+ and Maven.

## How it works

There are four stages, each small enough to test on its own:

1. **Tokenizer** scans the string into a flat list of tokens. It doesn't try to figure out unary vs binary minus — that's not its job.
2. **ShuntingYard** converts the tokens to RPN. The textbook algorithm needed three tweaks: disambiguating unary minus by tracking whether the previous token could end an operand, counting function arguments with a parallel stack so variadic functions resolve to a real arity, and checking paren balance while draining the operator stack.
3. **Evaluator** is a stack machine over the RPN. Pop operands, push results, resolve variables from the map you pass in.
4. **Expression** is the public face — `compile` parses to RPN and hands back something reusable, `evaluate` runs it.

Precedence, high to low: unary minus, `^`, `* / %`, `+ -`.

## Notes

I split the pipeline into four stages mainly so each one is testable in isolation — the shunting-yard step is the fiddly part and I wanted to be able to assert on the RPN output directly without dragging evaluation into it.

It only does `double` arithmetic, so the usual floating-point caveats apply and there's no arbitrary precision. No user-defined functions either — the function set is fixed in code. If I extend it, custom functions and maybe a `BigDecimal` mode are the obvious next steps.

## License

MIT — see [LICENSE](LICENSE).
