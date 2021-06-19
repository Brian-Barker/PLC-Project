package plc.project;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigInteger;
import java.math.BigDecimal;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> field1 = new ArrayList<>();
        List<Ast.Method> method1 = new ArrayList<>();

        while (peek("LET")) {
            field1.add(parseField());
        }

        while (peek("DEF")) {
            Ast.Method m1 = parseMethod();
            method1.add(m1);
        }

        return new Ast.Source(field1, method1);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {

        match ("LET");
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier", tokens.index);
        }
        else {
            String variable = tokens.get(-1).getLiteral();
            Optional<Ast.Expr> value = Optional.empty();

            if (match("=")) {
                value = Optional.ofNullable(parseExpression());
                if (!value.isPresent()) { //Make sure there is actually an expression
                    throw new ParseException("Expected Expression", tokens.index);
                }
            }

            if (!match(";")) {
                throw new ParseException("Missing Semicolon", tokens.index);
            }
            return new Ast.Field(variable, value);
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        String variable1;
        List<String> str = new ArrayList<>();
        List<Ast.Stmt> stmt = new ArrayList<>();

        match("DEF");

        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier", tokens.index);
        }

        variable1 = tokens.get(-1).getLiteral();

        if (!match("(")) {
            throw new ParseException("Expected opening parenthesis.", tokens.index);
        }

        if (match(Token.Type.IDENTIFIER)) {
            str.add(tokens.get(-1).getLiteral());
            while (match(",")) {
                if (!match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected identifier.", tokens.index);
                }
                str.add(tokens.get(-1).getLiteral());
            }
        }

        if (!match(")")) {
            throw new ParseException("Expected closing parenthesis.", tokens.index);
        }

        if (!match("DO")) {
            throw new ParseException("Expected \"DO\".", tokens.index);
        }

        while(tokens.has(0) && !peek("END")) {
            stmt.add(parseStatement());
        }

        if (!match("END")) {
            throw new ParseException("Expected \"END\".", tokens.index);
        }

        return new Ast.Method(variable1, str, stmt);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement();
        }
        else if (peek("IF")) {
            return parseIfStatement();
        }
        else if (peek("FOR")) {
            return parseForStatement();
        }
        else if (peek("WHILE")) {
            return parseWhileStatement();
        }
        else if (peek("RETURN")) {
            return parseReturnStatement();
        }
        else {
            Ast.Expr left = parseExpression();
            if (match("=")) {
                Ast.Expr right = parseExpression();
                if (match(";")) {
                    return new Ast.Stmt.Assignment(left, right);
                }
            }
            if (!match(";")) {
                throw new ParseException("Missing Semicolon", tokens.index);
            }
            return new Ast.Stmt.Expression(left);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {

        match ("LET");
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier", tokens.index);
        }

        String variable = tokens.get(-1).getLiteral();
        Optional<Ast.Expr> value = Optional.empty();

        if (match("=")) {
            value = Optional.ofNullable(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected Semicolon", tokens.index);
        }
        return new Ast.Stmt.Declaration(variable, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        List<Ast.Stmt> stmt1 = new ArrayList<>();
        List<Ast.Stmt> stmt2 = new ArrayList<>();

        match("IF");
        Ast.Expr expr1 = parseExpression();
        if (match("DO")) {
            stmt1.add(parseStatement());
        }
        else {
            throw new ParseException("Expected DO", tokens.index);
        }

        if (match("ELSE")) {
            stmt2.add(parseStatement());
        }

        if (match("END")) {
            return new Ast.Stmt.If(expr1, stmt1, stmt2);
        }
        else {
            throw new ParseException("Expected END", tokens.index);
        }
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        Ast.Expr expr1;
        List<Ast.Stmt> stmt1 = new ArrayList<>();

        match ("FOR");
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier", tokens.index);
        }
        String variable = tokens.get(-1).getLiteral();

        if (match("IN")) {
            expr1 = parseExpression();
        }
        else {
            throw new ParseException("Expected IN", tokens.index);
        }

        if (match("DO")) {
            stmt1.add(parseStatement());
        }
        else {
            throw new ParseException("Expected DO", tokens.index);
        }

        if (match("END")) {
            return new Ast.Stmt.For(variable, expr1, stmt1);
        }
        else {
            throw new ParseException("Expected END", tokens.index);
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        List<Ast.Stmt> stmt1 = new ArrayList<>();

        match("WHILE");
        Ast.Expr expr1 = parseExpression();
        if (match("DO")) {
            Ast.Stmt s1 = parseStatement();
            stmt1.add(s1);
        }
        else {
            throw new ParseException("Expected DO", tokens.index);
        }

        if (match("END")) {
            return new Ast.Stmt.While(expr1, stmt1);
        }
        else {
            throw new ParseException("Expected END", tokens.index); //FLAG
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expr expr1 = parseExpression();
        if (match(";")) {
            return new Ast.Stmt.Return(expr1);
        }
        else {
            throw new ParseException("Expected Semicolon", tokens.index);
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        Ast.Expr left = parseLogicalExpression();
        return left;
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr left = parseEqualityExpression();
        Ast.Expr right;
        String logical;

        if (peek("AND") || peek("OR")) {
            if (!match("AND"))
                match("OR");

            logical = tokens.get(-1).getLiteral();

            right = parseExpression();
            return new Ast.Expr.Binary(logical, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr left = parseAdditiveExpression();
        String equality;

        if (match("<")) {
            equality = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();

            return new Ast.Expr.Binary(equality, left, right);
        }
        else if (match(">")) {
            equality = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();

            return new Ast.Expr.Binary(equality, left, right);
        }
        else if (match(">=")) {
            equality = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();

            return new Ast.Expr.Binary(equality, left, right);
        }
        else if (match("<=")) {
            equality = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();

            return new Ast.Expr.Binary(equality, left, right);
        }
        else if (match("==")) {
            equality = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();

            return new Ast.Expr.Binary(equality, left, right);
        }
        else if (match("!=")) {
            equality = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();

            return new Ast.Expr.Binary(equality, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr left = parseMultiplicativeExpression();
        String additive;

        if (match("+")) {
            additive = tokens.get(-1).getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();

            return new Ast.Expr.Binary(additive, left, right);
        }
        else if (match("-")) {
            additive = tokens.get(-1).getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();

            return new Ast.Expr.Binary(additive, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr left = parseSecondaryExpression(null);
        String multiplicative;

        if (match("*")) {
            multiplicative = tokens.get(-1).getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();

            return new Ast.Expr.Binary(multiplicative, left, right);
        }
        else if (match("/")) {
            multiplicative = tokens.get(-1).getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();

            return new Ast.Expr.Binary(multiplicative, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression(Ast.Expr previousRef) throws ParseException {
        Ast.Expr primaryExpr = parsePrimaryExpression(previousRef);
        String identName = tokens.get(-1).getLiteral();

        if (match(".")) {
            return parseSecondaryExpression(primaryExpr);
        } else {
            return primaryExpr;
        }
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression(Ast.Expr previousRef) throws ParseException {
        // Check matches and finish returns
        if (match("TRUE")) {
            return new Ast.Expr.Literal(true);
        }
        else if (match("FALSE")) {
            return new Ast.Expr.Literal(false);
        }
        else if (match("NIL")) {
            return new Ast.Expr.Literal(null);
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if (name.matches("\\d+")) {
                throw new ParseException("Invalid Name.", (tokens.index)-1);
            }

            if (match("(")) {
                List<Ast.Expr> expressList = new ArrayList<>();
                while (tokens.has(0) && !peek(")")) {
                    Ast.Expr expression = parseExpression();
                    expressList.add(expression);
                    if (!match(",")) {
                        break;
                    } else {
                        if (!peek(Token.Type.IDENTIFIER)) {
                            throw new ParseException("Expected identifier.", tokens.index);
                        }
                    }
                }
                if (!match(")")) {
                    throw new ParseException("Expected closing parenthesis.", tokens.index);
                }
                if (previousRef == null) {
                    return new Ast.Expr.Function(Optional.empty(), name, expressList);
                } else {
                    return new Ast.Expr.Function(Optional.of(previousRef), name, expressList);
                }
            }
            if (previousRef == null) {
                return new Ast.Expr.Access(Optional.empty(), name);
            } else {
                return new Ast.Expr.Access(Optional.of(previousRef), name);
            }
        }
        else if (match(Token.Type.INTEGER)) {
            BigInteger num = new BigInteger(tokens.get(-1).getLiteral());
            return new Ast.Expr.Literal(num);
        }
        else if (match(Token.Type.DECIMAL)) {
            BigDecimal num = new BigDecimal(tokens.get(-1).getLiteral());
            return new Ast.Expr.Literal(num);
        }
        else if (match(Token.Type.CHARACTER)) {
            String character = replaceEscape( tokens.get(-1).getLiteral().replaceAll("'", "") );
            return new Ast.Expr.Literal(character.charAt(0));
        }
        else if (match(Token.Type.STRING)) {
            String str = replaceEscape( tokens.get(-1).getLiteral().replaceAll("\"", "") );
            return new Ast.Expr.Literal(str);
        }
        else if (match("(")) {
            Ast.Expr expr = parseExpression();
            if (!match(")")) {
                //System.out.print((tokens.index)-1);
                throw new ParseException("Expected closing parenthesis.", (tokens.index)-1);
            }
            return new Ast.Expr.Group(expr);
        }
        else {
            throw new ParseException("Invalid Primary Expression", tokens.index);
            // Check cannot access Token.getIndex() as suggested in Parser Doc
        }
    }

    private String replaceEscape(String str) {
        return str.replaceAll("\\\\b", "\b")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\r", "\r")
                .replaceAll("\\\\t", "\t")
                .replaceAll("\\\\'", "\'")
                .replaceAll("\\\\\"", "\"")
                .replaceAll("\\\\\\\\", "\\\\");
    }
    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for ( int i = 0; i < patterns.length; i++ ) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " +
                                         patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
