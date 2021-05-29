package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(

                //Provided
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),

                //Added
                Arguments.of("Number in-between", "zero1two", true),
                Arguments.of("Hyphen in-between", "hyp-hen", true),
                Arguments.of("Hyphen at End", "hyphen-", true),
                Arguments.of("All CAPS", "CAPS", true),
                Arguments.of("All Lower", "lower", true),
                Arguments.of("Leading Backslash", "\\backslash", false),
                Arguments.of("Mid Backslash", "back\\slash", false),
                Arguments.of("Ending Backslash", "backslash\\", false),
                Arguments.of("Backslash Throughout", "\\back\\slash\\", false),
                Arguments.of("Leading Forward Slash", "/forwardslash", false),
                Arguments.of("Mid Forward Slash", "forward/slash", false),
                Arguments.of("Ending Forward Slash", "forwardslash/", false),
                Arguments.of("Forward Slash Throughout", "/forward/slash/", false),
                Arguments.of("Hyphen Throughout", "-hy1p-h3n-", false),
                Arguments.of("Just One", "1", false),
                Arguments.of("Leading Zero", "0inFront", false),
                Arguments.of("Leading Nine", "9inFront", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                //Provided
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Signed Decimal", "-1.0", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),

                //Added
                Arguments.of("Zero", "0", true),
                Arguments.of("Nine", "9", true),
                Arguments.of("Zero at end", "10", true),
                Arguments.of("Multiple Digits", "123", true),
                Arguments.of("Zeros Before Decimal", "000.123", false),
                Arguments.of("Zeros After Decimal", "123.000", false),
                Arguments.of("Positive Sign", "+1.0", false),
                Arguments.of("Fraction Whole Number", "10/5", false),
                Arguments.of("Fraction Decimal Number", "10.0/3.0", false),
                Arguments.of("Leading Decimal 2", ".56", false),
                Arguments.of("Trailing Decimal 2", "12.", false),
                Arguments.of("Positive Sign w/ Leading Decimal", "+.5", false),
                Arguments.of("Positive Sign w/ Trailing Decimal", "+1.", false),
                Arguments.of("Negative Sign w/ Leading Decimal", "-.5", false),
                Arguments.of("Negative Sign w/ Trailing Decimal", "-1.", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                //Provided
                Arguments.of("Integer", "1", false),
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),

                //Added
                Arguments.of("Zero", "0", false),
                Arguments.of("Nine", "9", false),
                Arguments.of("Zero at end", "10", false),
                Arguments.of("Multiple Digits", "123", false),
                Arguments.of("Zeros Before Decimal", "000.123", true),
                Arguments.of("Zeros After Decimal", "123.000", true),
                Arguments.of("Positive Sign", "+1.0", true),
                Arguments.of("Fraction Whole Number", "10/5", false),
                Arguments.of("Fraction Decimal Number", "10.0/3.0", false),
                Arguments.of("Leading Decimal 2", ".56", false),
                Arguments.of("Trailing Decimal 2", "12.", false),
                Arguments.of("Positive Sign w/ Leading Decimal", "+.5", false),
                Arguments.of("Positive Sign w/ Trailing Decimal", "+1.", false),
                Arguments.of("Negative Sign w/ Leading Decimal", "-.5", false),
                Arguments.of("Negative Sign w/ Trailing Decimal", "-1.", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                //Provided
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false),

                //Added
                Arguments.of("Anchor: Word", "\'\\b\'", true),
                Arguments.of("Escape: Carriage Return", "\'\\r\'", true),
                Arguments.of("Escape: Tab", "\'\\t\'", true),
                Arguments.of("Escape: Single Quote", "\'\\'\'", true),
                Arguments.of("Escape: Double Quote", "\'\\\"\'", true),
                Arguments.of("Escape: Backslash", "\'\\\\'", true),
                Arguments.of("Numeric 0", "\'0\'", true),
                Arguments.of("Numeric 9", "\'9\'", true),
                Arguments.of("No Quotes", "c", false),
                Arguments.of("Only Left Quote", "\'c", false),
                Arguments.of("Only Right Quote", "c\'", false),
                Arguments.of("Numeric Multiple", "\'123\'", false),
                Arguments.of("Quote Entered", "\''\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                //Provided
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),

                //Added
                Arguments.of("All Caps", "\"ABC\"", true),
                Arguments.of("Upper/Lower Case", "\"AbC\"", true),
                Arguments.of("Anchor: Word", "\"\\b\"", true),
                Arguments.of("Escape: Carriage Return", "\"\\r\"", true),
                Arguments.of("Escape: Tab", "\"\\t\"", true),
                Arguments.of("Escape: Single Quote", "\"\\'\"", true),
                Arguments.of("Escape: Double Quote", "\"\\\"\"", true),
                Arguments.of("Escape: Backslash", "\"\\\\\"", true),
                Arguments.of("Numeric 0", "\"0\"", false),
                Arguments.of("Numeric 9", "\"9\"", false),
                Arguments.of("Numeric Multiple", "\"123\"", false),
                Arguments.of("Double Quote Entered", "\"\"\"", false),
                Arguments.of("Unterminated Closing", "\"Closing unterminated", false),
                Arguments.of("Unterminated Opening", "Opening unterminated\"", false),
                Arguments.of("Double Unterminated", "Double Unterminated", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                //Provided
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "<=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),

                //Added
                Arguments.of("Character 2", ")", true),
                Arguments.of("Comparison Greater than", ">", true),
                Arguments.of("Comparison Greater than Equal", ">=", true),
                Arguments.of("Comparison Less than", "<", true),
                Arguments.of("Comparison Not Equal", "!=", true),
                Arguments.of("Comparison Equal", "==", true),
                Arguments.of("Equal", "=", true),
                Arguments.of("Shift Left", "<<", true),
                Arguments.of("Shift Right", ">>", true),
                Arguments.of("Bitwise AND", "&", true),
                Arguments.of("Logical AND", "&&", true),
                Arguments.of("Logical OR", "||", true),
                Arguments.of("Bitwise OR Inclusive", "|", true),
                Arguments.of("Bitwise OR Exclusive", "^", true),
                Arguments.of("Multiplication", "*", true),
                Arguments.of("Division", "/", true),
                Arguments.of("Addition", "+", true),
                Arguments.of("Subtraction", "-", true),
                Arguments.of("Percent Sign", "%", true),
                Arguments.of("Exclaimation", "!", true),
                Arguments.of("Question", "?", true),
                Arguments.of("Colon", ":", true),
                Arguments.of("Semicolon", ";", true),
                Arguments.of("Hash", "#", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "I'm #1!", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "I'm", 0),
                        new Token(Token.Type.OPERATOR, "#", 4),
                        new Token(Token.Type.INTEGER, "1", 5),
                        new Token(Token.Type.INTEGER, "!", 6)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
