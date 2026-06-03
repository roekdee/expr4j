# expr4j

A small, dependency-free Java library that parses and evaluates math expressions using the shunting-yard algorithm.

![CI](https://github.com/roekdee/expr4j/actions/workflows/ci.yml/badge.svg)

## Features

- Operators: `+` `-` `*` `/` `%` and `^` (exponent, right-associative)
- Correct precedence and associativity (e.g. `2^3^2 == 512`, `10-2-3 == 5`)
- Unary minus (`-x`, `-(a+b)`, `2--3`) and a no-op unary plus
- Parentheses, with clear errors on imbalance
- Named variables bound at evaluation time (parse once, evaluate many)
- Built-in functions: `sqrt`, `abs`, `min`, `max`, `sin`, `cos`, `log`
  (`min`/`max` are variadic)
- Decimal and scientific-notation literals (`1.5e3`, `1.25e-2`)
- A single `ExpressionException` for all malformed input: unbalanced parentheses,
  unknown tokens, unbound variables, wrong arity, and division/modulo by zero
- Zero runtime dependencies; JUnit 5 only for tests

## Usage

```java
import com.roekdee.expr4j.Expression;
import java.util.Map;

// One-shot evaluation
double a = Expression.evaluate("2 + 3 * 4");      // 14.0
double b = Expression.evaluate("sqrt(abs(-16))"); // 4.0

// Compile once, evaluate repeatedly with different variable bindings
Expression poly = Expression.compile("x^2 + y");
double c = poly.evaluate(Map.of("x", 3.0, "y", 1.0)); // 10.0
double d = poly.evaluate(Map.of("x", 2.0, "y", 1.0)); // 5.0

// Variadic functions
double e = Expression.evaluate("max(1, 2, 3)");   // 3.0
```

Malformed input throws `ExpressionException` with a descriptive message:

```java
Expression.evaluate("(1 + 2");  // ExpressionException: Unbalanced parentheses: missing ')'
Expression.evaluate("1 / 0");   // ExpressionException: Division by zero
Expression.evaluate("x + 1");   // ExpressionException: Unbound variable 'x'
```

## Build & test

```bash
mvn test
```

Requires JDK 17+ and Maven.

## Design notes

The pipeline has four small, independently testable stages:

1. **`Tokenizer`** — scans the source string into a flat list of `Token`s
   (numbers, identifiers, operators, parentheses, commas). It is context-free
   and does not try to classify unary vs. binary operators.

2. **`ShuntingYard`** — Dijkstra's shunting-yard algorithm converts the infix
   token stream into Reverse Polish Notation (`RpnToken`s). It extends the
   textbook version in three ways:
   - **Unary minus** is disambiguated by tracking whether the previous token
     could end an operand; when a `-` (or `+`) appears where an operand is
     expected, it is treated as unary.
   - **Function calls** are pushed as operators and their argument lists are
     counted with a parallel stack of "argument frames", so variadic functions
     like `min` and `max` resolve to a concrete arity.
   - **Parenthesis balance** is validated while draining the operator stack,
     reporting which side is missing.

3. **`Evaluator`** — a straightforward stack machine over the RPN stream.
   Operators pop their operands and push the result; function calls pop their
   counted arguments; variables are resolved against the binding map supplied at
   evaluation time. Division/modulo by zero and arity mismatches fail loudly.

4. **`Expression`** — the public facade. `compile` parses once into RPN and
   returns a reusable object; `evaluate` runs the stack machine, optionally with
   variables. Static `Expression.evaluate(...)` overloads cover the one-shot case.

Precedence (high to low): unary minus → `^` → `* / %` → `+ -`. `^` is the only
right-associative binary operator.

## Tech

- Java 17 (records, sealed interfaces, switch expressions)
- Maven
- JUnit 5 (Jupiter, parameterized tests)
- GitHub Actions CI (Temurin 17 on Ubuntu)

## License

MIT — see [LICENSE](LICENSE).
