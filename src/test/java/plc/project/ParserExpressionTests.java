package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTests {

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Stmt.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Stmt.Expression(new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList()))
                ),
                Arguments.of("Missing Semicolon (Should Fail)",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Stmt.Expression(new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList()))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Stmt.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.empty(), "name"),
                                new Ast.Expr.Access(Optional.empty(), "value")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expr.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal True",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expr.Literal(Boolean.TRUE)
                ),
                Arguments.of("Boolean Literal False (Added)",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "FALSE", 0)),
                        new Ast.Expr.Literal(Boolean.FALSE)
                ),
                Arguments.of("Literal NIL (Added)",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "NIL", 0)),
                        new Ast.Expr.Literal(null)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expr.Literal(new BigInteger("1"))
                ),
                Arguments.of("Integer Literal (Added)",
                        Arrays.asList(new Token(Token.Type.INTEGER, "123456789123456789123456789", 0)),
                        new Ast.Expr.Literal(new BigInteger("123456789123456789123456789"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expr.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Decimal Literal (Added)",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "123456789123456789123456789.9999999", 0)),
                        new Ast.Expr.Literal(new BigDecimal("123456789123456789123456789.9999999"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expr.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expr.Literal("string")
                ),
                Arguments.of("String Literal (Added)",
                        Arrays.asList(new Token(Token.Type.STRING, "\"This is a string\"", 0)),
                        new Ast.Expr.Literal("This is a string")
                ),
                Arguments.of("String Escape \b (ADDED)",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\bWorld!\"", 0)),
                        new Ast.Expr.Literal("Hello,\bWorld!")
                ),
                Arguments.of("Escape Character in String",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expr.Literal("Hello,\nWorld!")
                ),
                Arguments.of("String Escape \r (ADDED)",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\rWorld!\"", 0)),
                        new Ast.Expr.Literal("Hello,\rWorld!")
                ),
                Arguments.of("String Escape \t (ADDED)",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\tWorld!\"", 0)),
                        new Ast.Expr.Literal("Hello,\tWorld!")
                ),
                Arguments.of("String Escape \' (ADDED)",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\'World!\"", 0)),
                        new Ast.Expr.Literal("Hello,\'World!")
                ),
                Arguments.of("String Escape \" (ADDED)",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\\"World!\"", 0)),
                        new Ast.Expr.Literal("Hello,\"World!")
                ),
                Arguments.of("String Escape \\ (ADDED)",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\\\World!\"", 0)),
                        new Ast.Expr.Literal("Hello,\\World!")
                ),
                Arguments.of("Escape Character \b (ADDED)",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\b'", 0)),
                        new Ast.Expr.Literal('\b')
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\n'", 0)),
                        new Ast.Expr.Literal('\n')
                ),
                Arguments.of("Escape Character \r (ADDED)",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\r'", 0)),
                        new Ast.Expr.Literal('\r')
                ),
                Arguments.of("Escape Character \t (ADDED)",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\t'", 0)),
                        new Ast.Expr.Literal('\t')
                ),
                Arguments.of("Escape Character \' (ADDED)",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\''", 0)),
                        new Ast.Expr.Literal('\'')
                ),
                Arguments.of("Escape Character \" (ADDED)",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\\"'", 0)),
                        new Ast.Expr.Literal('\"')
                ),
                Arguments.of("Escape Character \\ (ADDED)",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'\\\\'", 0)),
                        new Ast.Expr.Literal('\\')
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expr.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        ))
                ),
                Arguments.of("Missing Closing Parenthesis (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //(expr1
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Missing Expression (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //()
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, ")", 1)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Access(Optional.empty(), "empty"))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expr.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 AND expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Or (ADDED)",
                        Arrays.asList(
                                //expr1 OR expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "OR", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expr.Binary("OR",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Less Than (ADDED)",
                        Arrays.asList(
                                //expr1 < expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("<",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Less Than Equal To (ADDED)",
                        Arrays.asList(
                                //expr1 <= expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expr.Binary("<=",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Greater Than (ADDED)",
                        Arrays.asList(
                                //expr1 < expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary(">",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Greater Than Equal To (ADDED)",
                        Arrays.asList(
                                //expr1 <= expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expr.Binary(">=",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expr.Binary("==",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Inequality",
                        Arrays.asList(
                                //expr1 != expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "!=", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expr.Binary("!=",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Subtraction (ADDED)",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("-",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("*",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Division (ADDED)",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "/", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("/",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary AND OR (Should Fail)",
                        Arrays.asList(
                                //expr1 AND OR expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "OR", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 13)
                        ),
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Missing Parenthesis (Should Fail)",
                        Arrays.asList(
                                //(expr1 + expr2
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.IDENTIFIER, ")", 14)
                        ),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary And Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 AND
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6)
                        ),
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Or Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 OR
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "OR", 6)
                        ),
                        new Ast.Expr.Binary("OR",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Less Than Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 <
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<", 6)
                        ),
                        new Ast.Expr.Binary("<",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Less Than Equal To Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 <=
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "<=", 6)
                        ),
                        new Ast.Expr.Binary("<=",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Greater Than Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 < expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">", 6)
                        ),
                        new Ast.Expr.Binary(">",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Greater Than Equal To Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 <=
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, ">=", 6)
                        ),
                        new Ast.Expr.Binary(">=",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 ==
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6)
                        ),
                        new Ast.Expr.Binary("==",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Inequality Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 !=
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "!=", 6)
                        ),
                        new Ast.Expr.Binary("!=",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 +
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6)
                        ),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Subtraction Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 +
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "-", 6)
                        ),
                        new Ast.Expr.Binary("-",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 *
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6)
                        ),
                        new Ast.Expr.Binary("*",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Division Missing Operand (ADDED) (SHOULD FAIL)",
                        Arrays.asList(
                                //expr1 *
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "/", 6)
                        ),
                        new Ast.Expr.Binary("/",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary AND Multiple Operands",
                        Arrays.asList(
                                //expr1 AND expr2 AND expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10),
                                new Token(Token.Type.IDENTIFIER, "AND", 16),
                                new Token(Token.Type.IDENTIFIER, "expr3", 20)
                        ),
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                (new Ast.Expr.Binary("AND",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                ))
                        )
                ),
                Arguments.of("Binary AND Multiple Operands 2",
                        Arrays.asList(
                                //expr1 AND expr2 AND expr3 AND expr4
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10),
                                new Token(Token.Type.IDENTIFIER, "AND", 16),
                                new Token(Token.Type.IDENTIFIER, "expr3", 20),
                                new Token(Token.Type.IDENTIFIER, "AND", 26),
                                new Token(Token.Type.IDENTIFIER, "expr4", 30)
                        ),
                        new Ast.Expr.Binary("AND",
                                (new Ast.Expr.Binary("AND",
                                        new Ast.Expr.Access(Optional.empty(), "expr1"),
                                        new Ast.Expr.Access(Optional.empty(), "expr2")
                                )),
                                (new Ast.Expr.Binary("AND",
                                        new Ast.Expr.Access(Optional.empty(), "expr3"),
                                        new Ast.Expr.Access(Optional.empty(), "expr4")
                                ))
                        )
                ),
                Arguments.of("Binary AND/OR Multiple Operands",
                        Arrays.asList(
                                //expr1 AND expr2 OR expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10),
                                new Token(Token.Type.IDENTIFIER, "OR", 16),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19)
                        ),
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                (new Ast.Expr.Binary("OR",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                ))
                        )
                ),
                Arguments.of("Binary OR Multiple Operands",
                        Arrays.asList(
                                //expr1 OR expr2 OR expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "OR", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.IDENTIFIER, "OR", 15),
                                new Token(Token.Type.IDENTIFIER, "expr3", 18)
                        ),
                        new Ast.Expr.Binary("OR",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                (new Ast.Expr.Binary("OR",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                ))
                        )
                ),
                Arguments.of("Binary AND / Equality",
                        Arrays.asList(
                                //expr1 == (expr2 AND expr3)
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.OPERATOR, "(", 9),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10),
                                new Token(Token.Type.IDENTIFIER, "AND", 16),
                                new Token(Token.Type.IDENTIFIER, "expr3", 20),
                                new Token(Token.Type.OPERATOR, ")", 25)
                        ),
                        new Ast.Expr.Binary("==",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Group((new Ast.Expr.Binary("AND",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                )))
                        )
                ),
                Arguments.of("Binary + Multiple Operands",
                        Arrays.asList(
                                //expr1 + expr2 + expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.IDENTIFIER, "+", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                (new Ast.Expr.Binary("+",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                ))
                        )
                ),
                Arguments.of("Binary / Multiple Operands",
                        Arrays.asList(
                                //expr1 / expr2 / expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "/", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.IDENTIFIER, "/", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expr.Binary("/",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                (new Ast.Expr.Binary("/",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                ))
                        )
                ),
                Arguments.of("Binary * / Multiple Operands",
                        Arrays.asList(
                                //expr1 * expr2 / expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.IDENTIFIER, "/", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expr.Binary("*",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                (new Ast.Expr.Binary("/",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                ))
                        )
                ),
                Arguments.of("Binary * + Multiple Operands",
                        Arrays.asList(
                                //expr1 * expr2 + expr3
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8),
                                new Token(Token.Type.IDENTIFIER, "+", 14),
                                new Token(Token.Type.IDENTIFIER, "expr3", 16)
                        ),
                        new Ast.Expr.Binary("*",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                (new Ast.Expr.Binary("+",
                                        new Ast.Expr.Access(Optional.empty(), "expr2"),
                                        new Ast.Expr.Access(Optional.empty(), "expr3")
                                ))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expr.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expr.Access(Optional.empty(), "name")
                ),
                Arguments.of("Field Access",
                        Arrays.asList(
                                //obj.field
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "field", 4)
                        ),
                        new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "field")
                ),
                Arguments.of("Multiple Field Access",
                        Arrays.asList(
                                //obj.field
                                new Token(Token.Type.IDENTIFIER, "obj1", 0),
                                new Token(Token.Type.OPERATOR, ".", 4),
                                new Token(Token.Type.IDENTIFIER, "field1", 5),
                                new Token(Token.Type.OPERATOR, ".", 11),
                                new Token(Token.Type.IDENTIFIER, "field2", 12)
                        ),
                        new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj1")), "field1")), "field2")
                ),
                Arguments.of("Invalid Name (Should Fail)",
                        Arrays.asList(
                                //obj1.5
                                new Token(Token.Type.IDENTIFIER, "obj1", 0),
                                new Token(Token.Type.OPERATOR, ".", 4),
                                new Token(Token.Type.IDENTIFIER, "5", 5)
                        ),
                        //Arrays.asList(new Token(Token.Type.IDENTIFIER, "5", 0)),
                        new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj1")), "5")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expr.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList(
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2"),
                                new Ast.Expr.Access(Optional.empty(), "expr3")
                        ))
                ),
                Arguments.of("Single Argument",
                        Arrays.asList(
                                //name(expr1)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ")", 10)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList(
                                new Ast.Expr.Access(Optional.empty(), "expr1")
                        ))
                ),
                Arguments.of("Complex Argument (ADDED) (UNSURE)",
                        Arrays.asList(
                                //name(expr1/expr2)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, "/", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 11),
                                new Token(Token.Type.OPERATOR, ")", 16)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList(
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        ))
                ),
                Arguments.of("Method Call",
                        Arrays.asList(
                                //obj.method()
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "method", 4),
                                new Token(Token.Type.OPERATOR, "(", 10),
                                new Token(Token.Type.OPERATOR, ")", 11)
                        ),
                        new Ast.Expr.Function(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "method", Arrays.asList())
                ),
                Arguments.of("Method Call One Argument (ADDED) (UNSURE ABOUT TEST CASE)",
                        Arrays.asList(
                                //obj.method()
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "method", 4),
                                new Token(Token.Type.OPERATOR, "(", 10),
                                new Token(Token.Type.OPERATOR, "x", 11),
                                new Token(Token.Type.OPERATOR, ")", 12)
                        ),
                        new Ast.Expr.Function(Optional.of(new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "x")), "obj")), "method", Arrays.asList())
                ),
                Arguments.of("Trailing Comma (Should fail)",
                        Arrays.asList(
                                //method()
                                new Token(Token.Type.IDENTIFIER, "method", 0),
                                new Token(Token.Type.OPERATOR, "(", 6),
                                new Token(Token.Type.IDENTIFIER, "field", 7),
                                new Token(Token.Type.OPERATOR, ",", 12),
                                new Token(Token.Type.OPERATOR, ")", 13)
                        ),
                        null
                )
        );
    }


    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
