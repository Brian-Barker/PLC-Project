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
        if (!match(Token.Type.IDENTIFIER, ":", Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected \"Identifier : Identifier\"", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        String variable = tokens.get(-3).getLiteral();
        Optional<Ast.Expr> value = Optional.empty();

        String type = tokens.get(-1).getLiteral();

        if (match("=")) {
            value = Optional.ofNullable(parseExpression());
            if (!value.isPresent()) { //Make sure there is actually an expression
                throw new ParseException("Expected Expression", tokens.has(0) ? tokens.index : tokens.index-1);
            }
        }

        if (!match(";")) {
            throw new ParseException("Missing Semicolon", tokens.has(0) ? tokens.index : tokens.index-1);
        }
        return new Ast.Field(variable, type, value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        String name;
        List<String> parameters = new ArrayList<>();
        List<String> parameterTypeNames = new ArrayList<>();
        Optional<String> returnTypeName = Optional.empty();
        List<Ast.Stmt> statements = new ArrayList<>();

        match("DEF");

        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier", tokens.has(0) ? tokens.index : tokens.index-1);
        }
        name = tokens.get(-1).getLiteral();

        if (!match("(")) {
            throw new ParseException("Expected opening parenthesis.", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        if (match(Token.Type.IDENTIFIER, ":", Token.Type.IDENTIFIER)) {
            parameters.add(tokens.get(-3).getLiteral());
            parameterTypeNames.add(tokens.get(-1).getLiteral());
            while (match(",")) {
                if (!match(Token.Type.IDENTIFIER, ":", Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected \"Identifier : Type\"", tokens.has(0) ? tokens.index : tokens.index-1);
                } else {
                    parameters.add(tokens.get(-3).getLiteral());
                    parameterTypeNames.add(tokens.get(-1).getLiteral());
                }
            }
        }

        if (!match(")")) {
            throw new ParseException("Expected closing parenthesis.", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        if (match(":", Token.Type.IDENTIFIER)) {
            returnTypeName = Optional.ofNullable(tokens.get(-1).getLiteral());
        }

        if (!match("DO")) {
            throw new ParseException("Expected \"DO\".", tokens.has(0) ? tokens.index : tokens.index-1);
        }
        while(tokens.has(0) && !peek("END")) {
            statements.add(parseStatement());
        }

        if (!match("END")) {
            throw new ParseException("Expected \"END\".", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        return new Ast.Method(name, parameters, parameterTypeNames, returnTypeName, statements);
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
                throw new ParseException("Missing Semicolon", tokens.has(0) ? tokens.index : tokens.index-1);
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
            throw new ParseException("Expected Identifier", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        String variable = tokens.get(-1).getLiteral();
        Optional<Ast.Expr> value = Optional.empty();
        Optional<String> type = Optional.empty();

        if (match(":", Token.Type.IDENTIFIER)) {
            type = Optional.ofNullable(tokens.get(-1).getLiteral());
        }

        if (match("=")) {
            value = Optional.ofNullable(parseExpression());
        }

        if (!match(";")) {
            throw new ParseException("Expected Semicolon", tokens.has(0) ? tokens.index : tokens.index-1);
        }
        return new Ast.Stmt.Declaration(variable, type, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        List<Ast.Stmt> thenStmts = new ArrayList<>();
        List<Ast.Stmt> elseStmts = new ArrayList<>();

        match("IF");
        Ast.Expr condition = parseExpression();
        if (!match("DO")) {
            throw new ParseException("Expected DO", tokens.has(0) ? tokens.index : tokens.index-1);
        }
        while (tokens.has(0) && !peek("ELSE") && !peek("END")) {
            thenStmts.add(parseStatement());
        }

        while (match("ELSE")) {
            elseStmts.add(parseStatement());
        }

        if (!match("END")) {
            throw new ParseException("Expected END", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        return new Ast.Stmt.If(condition, thenStmts, elseStmts);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        Ast.Expr value;
        List<Ast.Stmt> stmts = new ArrayList<>();

        match("FOR");

        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier", tokens.has(0) ? tokens.index : tokens.index-1);
        }
        String name = tokens.get(-1).getLiteral();

        if (!match("IN")) {
            throw new ParseException("Expected \"IN\"", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        value = parseExpression();

        if (!match("DO")) {
            throw new ParseException("Expected \"DO\"", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        while(tokens.has(0) && !peek("END")) {
            stmts.add(parseStatement());
        }

        if (!match("END")) {
            throw new ParseException("Expected \"END\"", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        return new Ast.Stmt.For(name, value, stmts);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        Ast.Expr condition;
        List<Ast.Stmt> stmts = new ArrayList<>();

        match("WHILE");
        condition = parseExpression();

        if (!match("DO")) {
            throw new ParseException("Expected DO", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        while(tokens.has(0) && !peek("END")) {
            stmts.add(parseStatement());
        }

        if (!match("END")) {
            throw new ParseException("Expected END", tokens.has(0) ? tokens.index : tokens.index-1);
        }

        return new Ast.Stmt.While(condition, stmts);
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
            throw new ParseException("Expected Semicolon", tokens.has(0) ? tokens.index : tokens.index-1);
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

            right = parseLogicalExpression();
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
                throw new ParseException("Invalid Name.", tokens.has(0) ? tokens.index : tokens.index-1);
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
                            throw new ParseException("Expected identifier.", tokens.has(0) ? tokens.index : tokens.index-1);
                        }
                    }
                }
                if (!match(")")) {
                    throw new ParseException("Expected closing parenthesis.", tokens.has(0) ? tokens.index : tokens.index-1);
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
                throw new ParseException("Expected closing parenthesis.", tokens.has(0) ? tokens.index : tokens.index-1);
            }
            return new Ast.Expr.Group(expr);
        }
        else {
            throw new ParseException("Invalid Primary Expression", tokens.has(0) ? tokens.index : tokens.index-1);
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
