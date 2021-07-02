package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if ( ast.getValue().isPresent() ) {
            scope.defineVariable( ast.getName(), visit( ast.getValue().get() ) );
        } else {
            scope.defineVariable( ast.getName(), Environment.NIL );
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {

        visit ( ast.getExpression() );
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if ( ast.getValue().isPresent() ) {
            scope.defineVariable( ast.getName(), visit( ast.getValue().get() ) );
        } else {
            scope.defineVariable( ast.getName(), Environment.NIL );
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {

        System.out.print(ast.getValue());

        String literal = ast.getValue().toString();
        literal = ast.getValue().toString().substring(literal.indexOf("Ast.Expr.Literal{literal=") + 25);
        literal = literal.split("}")[0];

        return Environment.create(literal);
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        Boolean condition = requireType( Boolean.class, visit( ast.getCondition() ) );

        System.out.println(ast.toString());

        if (condition) ast.getThenStatements().forEach(this::visit);
        else ast.getElseStatements().forEach(this::visit);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        System.out.println(ast.getValue().toString());

        requireType( Iterable.class, visit( ast.getValue() ) ); //TODO

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while ( requireType( Boolean.class, visit( ast.getCondition() ) ) ) {
            try {
                scope = new Scope(scope);

                ast.getStatements().forEach(this::visit);
                /*for ( Ast.Stmt stmt : ast.getStatements() ) { //This does the same as the above statement
                    visit( stmt );
                }*/
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return( Environment.create( ast.getValue() ) );
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null){
            return Environment.NIL;
        }
        return Environment.create( ast.getLiteral() );
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit( ast.getExpression() );
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        String str = ast.getOperator();

        if (str.contains("AND")) {
            if (ast.getLeft().toString().contains("true") && ast.getRight().toString().contains("false")) {
                return Environment.create(false);
            }
            else if (ast.getLeft().toString().contains("false") && ast.getRight().toString().contains("true")) {
                return Environment.create(false);
            }
            else if (ast.getLeft().toString().contains("true") && ast.getRight().toString().contains("true")) {
                return Environment.create(true);
            }
            else if (ast.getLeft().toString().contains("false") && ast.getRight().toString().contains("false")) {
                return Environment.create(false);
            }
            return Environment.NIL;
        }
        else if (str.contains("OR")) {
            if (ast.getLeft().toString().contains("true") || ast.getRight().toString().contains("true")) {
                return Environment.create(true);
            }
            else if (ast.getLeft().toString().contains("false") && ast.getRight().toString().contains("false")) {
                return Environment.create(false);
            }
            return Environment.NIL;
        }

        String left = ast.getLeft().toString();
        String right = ast.getRight().toString();

        left = left.substring(left.lastIndexOf("=") + 1, left.lastIndexOf("}"));
        right = right.substring(right.lastIndexOf("=") + 1, right.lastIndexOf("}"));

        if (left.matches(".*\\d.*") || right.matches(".*\\d.*")) {
            BigDecimal leftBigDec = new BigDecimal(left);
            BigDecimal rightBigDec = new BigDecimal(right);

            //Note: Watch out for funneling expressions properly while using contains
            //Ex: <= will go into < as <= contains <, therefore, need to funnel <= before <

            if (str.contains("<=")) {
                if (leftBigDec.compareTo(rightBigDec) <= 0) {
                    return Environment.create(true);
                } else {
                    return Environment.create(false);
                }
            } else if (str.contains("<")) {
                if (leftBigDec.compareTo(rightBigDec) < 0) {
                    return Environment.create(true);
                } else {
                    return Environment.create(false);
                }
            } else if (str.contains(">=")) {
                if (leftBigDec.compareTo(rightBigDec) >= 0) {
                    return Environment.create(true);
                } else {
                    return Environment.create(false);
                }
            } else if (str.contains(">")) {
                if (leftBigDec.compareTo(rightBigDec) > 0) {
                    return Environment.create(true);
                } else {
                    return Environment.create(false);
                }
            } else if (str.contains("==")) {
                if (leftBigDec.compareTo(rightBigDec) == 0) {
                    return Environment.create(true);
                } else {
                    return Environment.create(false);
                }
            } else if (str.contains("!=")) {                        ///DOUBLE CHECK LOGIC
                if (leftBigDec.compareTo(rightBigDec) != 0) {
                    return Environment.create(true);
                } else {
                    return Environment.create(false);
                }
            }

            if (str.contains("+")) {
                if ((Math.round(leftBigDec.doubleValue()) == leftBigDec.doubleValue()) &&
                        Math.round(rightBigDec.doubleValue()) == rightBigDec.doubleValue()) {
                    BigInteger leftBigInt = leftBigDec.toBigInteger();
                    BigInteger rightBigInt = rightBigDec.toBigInteger();
                    return Environment.create(leftBigInt.add(rightBigInt));
                }
                return Environment.create(leftBigDec.add(rightBigDec));
            } else if (str.contains("-")) {
                return Environment.create(leftBigDec.subtract(rightBigDec));
            } else if (str.contains("*")) {
                return Environment.create(leftBigDec.multiply(rightBigDec));
            } else if (str.contains("/")) {
                return Environment.create(leftBigDec.divide(rightBigDec, BigDecimal.ROUND_HALF_UP));
            }
        }

        //Concatenation
        if (str.contains("+")) {
            return Environment.create(left + right);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        String fullVariable = ast.getName();

        /*while(ast.getReceiver().isPresent()) {
            ast = (Ast.Expr.Access) ast.getReceiver().get();
            fullVariable = ast.getName() + '.' + fullVariable;
        }
        Environment.PlcObject test = Environment.create(fullVariable);*/
        if (ast.getReceiver().isPresent()) {
            ast.getReceiver().
        }

        return Environment.create(fullVariable);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        String fullFunction = ast.getName();

        if (ast.getArguments().isEmpty()) {
            if (ast.getReceiver().isPresent()){
                fullFunction = "object." + fullFunction;
            }
            return Environment.create(fullFunction);
        }
        return Environment.NIL;
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
