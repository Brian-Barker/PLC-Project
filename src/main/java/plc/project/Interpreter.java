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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {


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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException(); //TODO
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
        throw new UnsupportedOperationException(); //TODO
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
        String str = ast.getExpression().toString();
        BigInteger bigIntStr1,bigIntStr2 = null;

        if (str.contains("operator")) {

            String leftLiteral = str;
            leftLiteral = ast.getExpression().toString().substring(leftLiteral.indexOf("left=Ast.Expr.Literal{literal=") + 30);
            leftLiteral = leftLiteral.split("}")[0];

            String rightLiteral = str;
            rightLiteral = ast.getExpression().toString().substring(rightLiteral.indexOf("right=Ast.Expr.Literal{literal=") + 31);
            rightLiteral = rightLiteral.split("}")[0];

            bigIntStr1 = new BigInteger(leftLiteral);
            bigIntStr2 = new BigInteger(rightLiteral);

            BigInteger res = bigIntStr1.add(bigIntStr2);

            return Environment.create(res);
        }

        String literal = str.replaceAll("[^0-9]", ""); //Need to get first literal
        bigIntStr1 = new BigInteger(literal);

        return Environment.create(bigIntStr1);
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

        while(ast.getReceiver().isPresent()) {
            ast = (Ast.Expr.Access) ast.getReceiver().get();
            fullVariable = ast.getName() + '.' + fullVariable;
        }

        return Environment.create(fullVariable);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException(); //TODO
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
