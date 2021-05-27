package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<Token>();
        while ( chars.has(chars.index) ) {
            tokens.add(lexToken());
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        if ( peek("\\s*") ) { //Whitespace
            handleWhitespace();
        }

        if ( peek("[A-Za-z_]") ) { //Identifier
            return lexIdentifier();
        } else if ( peek("[+\\-]") || peek("[0-9]") ) { //Number
            return lexNumber();
        } else if ( peek("[']") ) { //Character
            return lexCharacter();
        } else if ( peek("'\"' ([^\"\\n\\r\\\\] | '\\' [bnrt'\"\\\\])* '\"'") ) { //String
            return lexString();
        } else if ( peek("[<>!=] '='? | \\S") ) { //Operator
            return lexOperator();
        }
        throw new ParseException("Parse Exception: " + chars.get(chars.index), chars.index);
    }
    public Token lexIdentifier() {
        while ( match("[A-Za-z0-9_-]*") ); //Iterate through string until no longer matches, then use emit
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        boolean decimal = false;
        match("[+\\-]"); //There may be a single leading + or -
        while( match("[0-9]") ); //Get all leading digits
        if ( !match("\\.") ) { //If there's no decimal, we have an integer
            return chars.emit(Token.Type.INTEGER);
        }
        if ( !match("[0-9]") ) { //Must be at least one trailing digit after decimal
            throw new ParseException("Error Parsing Decimal: Invalid Trailing Decimal", chars.index);
        }
        while( match("[0-9]") ); //Get all remaining trailing digits
        return chars.emit(Token.Type.DECIMAL);
    }
//['] ([^'\n\r\\] | '\' [bnrt'"\\]) [']
    public Token lexCharacter() {
        if ( match("\\'((\\S)|(\\\\[bnrt\\'\\\"\\\\]))\\'") ) {
            return chars.emit(Token.Type.CHARACTER);
        }
        throw new ParseException("Error Parsing Character", chars.index);
    }

    public Token lexString() {
        if ( match("'\"' ([^\"\\n\\r\\\\] | '\\' [bnrt'\"\\\\])* '\"'") ) {
            return chars.emit(Token.Type.STRING);
        }
        throw new ParseException("Error Parsing String", chars.index);
    }

    public Token lexOperator() {
        if ( match("[<>!=] '='? | \\S") ) {
            return chars.emit(Token.Type.OPERATOR);
        }
        throw new ParseException("Error Parsing Operator", chars.index);
    }

    public void handleWhitespace() {
        if ( match("[A-Za-z_] [A-Za-z0-9_-]*") ) {
            return;
        }
        throw new ParseException("Error Parsing Whitespace", chars.index);
    }
    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for ( int i = 0; i < patterns.length; i++ ) {

            if ( !chars.has(i) ||
                    !String.valueOf(chars.get(i)).matches(patterns[i]) ) {

                return false;
            }

        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */

    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {

            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }

        }

        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
