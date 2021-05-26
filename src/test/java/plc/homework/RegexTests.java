package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RegexTests {

    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Caps", "theLegend27@gmail.com", true),
                Arguments.of("Underscore", "under_score@gmail.com", true),
                Arguments.of("Uppercase Domain", "shouting@GMAIL.com", true),
                Arguments.of("Short Domain", "foreign@email.nz", true),
                Arguments.of("Dash", "da-sh@gmail.com", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Missing @", "missinggmail.com", false),
                Arguments.of("Two .com's", "twodotcoms@gmail.com.com", false),
                Arguments.of("Invalid .com", "invaliddomain@gmail.c", false),
                Arguments.of("No organization name", "organizationfreedom@.com", false),
                Arguments.of("Empty Address", "@gmail.com", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("End Line Char", "123456789\n", true),
                Arguments.of("Whitespace", "12  34567 8901", true),
                Arguments.of("Escaped Chars", "012345\b789", true),
                Arguments.of("10 Chars", "0123456789", true),
                Arguments.of("20 Chars", "01234578901234567890", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),
                Arguments.of("8 Chars", "01234567", false),
                Arguments.of("9 Chars", "012345678", false),
                Arguments.of("21 Chars", "012345678901234567890", false),
                Arguments.of("22 Chars", "0123456789012345678901", false),
                Arguments.of("19 Chars", "0123456789012345678", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Empty", "[]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Single Space Characters", "[1, 2, 3]", true),
                Arguments.of("Varying Space Characters", "[1,\t2, 3]", true),
                Arguments.of("Negative Elements", "[-1,-2]", true),
                Arguments.of("New Line Space", "[1,\n2]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Multiple Space Characters", "[1,  2,3]", false),
                Arguments.of("Leading Space", "[ 1]", false),
                Arguments.of("Double List Item", "[1, 2 3, 4]", false),
                Arguments.of("Indeterminate Bracket", "[1,2", false),
                Arguments.of("Missing Commas", "[1 2 3]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                Arguments.of("Integer", "1", true),
                Arguments.of("Decimal", "123.456", true),
                Arguments.of("Negative", "-1.0", true),
                Arguments.of("Negative Integer", "-1", true),
                Arguments.of("Long", "123456789012345678901234567891234567891234567889123456789123456789", true),
                Arguments.of("Leading 0's", "0001", true),
                Arguments.of("Trailing 0's", "1.000", true),
                Arguments.of("Decimal Terminate", "1.", false),
                Arguments.of("Decimal Start", ".5", false),
                Arguments.of("Empty", "", false),
                Arguments.of("Double Decimal", "0.1.1", false),
                Arguments.of("Double Negative", "--1", false),
                Arguments.of("Non-Number", "a", false),
                Arguments.of("End Negative", "1-", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Punctuated/ Spaced", "\"Hello, World!\"", true),
                Arguments.of("Escape", "\"1\\t2\"", true),
                Arguments.of("Lone Escaped Char", "\"\n\"", true),
                Arguments.of("Weird Chars", "\"Ω\uD83D\uDC15.#\"", true),
                Arguments.of("Apostrophe", "\"'\\'\"", true),
                Arguments.of("Long String", "\"adsfjygjiuyg\\bjojo8785465468\\'djfhwi jehfiw ********** iiooooooooooohhhhhh hhhhhhhhhhyiiiiiiiiiiiiiiiiiiiiiiiiiiiyyyyyyyyyyyyyyyyyyyyeeeeeeeeeeeeeeeeeeeeeeeeerrrrrrrrrrrrrrrrrrrrrrrdddddddddddddddddddddddddddwiidih wbibwibfd\"", true),
                Arguments.of("Unterminate", "\"unterminated", false),
                Arguments.of("Extra Quote", "\"\"\"", false),
                Arguments.of("Double String", "\"\"\"\"", false),
                Arguments.of("Chars Preceeding Quotes", "a\"\"", false),
                Arguments.of("Chars Exceeding Quotes", "\"\"a", false),
                Arguments.of("Bad Escape", "\"invalid\\escape\"", false),
                Arguments.of("No String", "", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
