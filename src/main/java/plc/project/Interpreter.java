package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);
    //private Optional<Ast.Expr> ;

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("logarithm", 1, arg_list -> {
          if ( !( arg_list.get(0).getValue() instanceof BigDecimal) ) {
              throw new RuntimeException("Expected type BigDecimal, received"
                      + arg_list.get(0).getValue().getClass().getName() + ".");
          }
          BigDecimal bd1 = (BigDecimal) arg_list.get(0).getValue();
          BigDecimal bd2 = requireType(BigDecimal.class, Environment.create(arg_list.get(0).getValue()));
          BigDecimal res = BigDecimal.valueOf(Math.log(bd2.doubleValue()));

          return Environment.create(res);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        ast.getFields().forEach(this::visit);
        ast.getMethods().forEach(this::visit);

        return scope.lookupFunction( "main", 0 ).invoke(Collections.emptyList());
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
        java.util.function.Function<List<Environment.PlcObject>, Environment.PlcObject> function = arguments -> {
            scope = new Scope(scope);

            for (int i = 0; i < arguments.size(); ++i) {
                scope.defineVariable(ast.getParameters().get(i), arguments.get(i));
            }

            try {
                ast.getStatements().forEach(this::visit);
            } catch (Return returnValue) {
                scope = scope.getParent();
                return returnValue.value;
            }

            scope = scope.getParent();
            return Environment.NIL;
        };

        scope.defineFunction(ast.getName(), ast.getParameters().size(), function);

        return Environment.NIL;
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
        if (ast.getReceiver().getClass() != Ast.Expr.Access.class) {
            throw new RuntimeException("Non-assignable type. Expected Ast.Expr.Access, Got " + ast.getReceiver().getClass());
        }

        if (((Ast.Expr.Access) ast.getReceiver()).getReceiver().isPresent()) {
            visit(((Ast.Expr.Access) ast.getReceiver()).getReceiver().get()).setField( ((Ast.Expr.Access) ast.getReceiver()).getName(), visit(ast.getValue()) );
        } else {
            scope.lookupVariable(((Ast.Expr.Access) ast.getReceiver()).getName()).setValue(visit(ast.getValue()));
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        Boolean condition = requireType( Boolean.class, visit( ast.getCondition() ) );

        if (condition) ast.getThenStatements().forEach(this::visit);
        else ast.getElseStatements().forEach(this::visit);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {

        for (Object element : requireType(Iterable.class, visit(ast.getValue())) ) {
            scope = new Scope(scope);

            scope.defineVariable( ast.getName(), (Environment.PlcObject) element );
            ast.getStatements().forEach(this::visit);

            scope = scope.getParent();
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while ( requireType( Boolean.class, visit( ast.getCondition() ) ) ) {
            try {
                scope = new Scope(scope);

                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return( visit(ast.getValue()) );
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
        if (ast.getOperator().equals("AND")) {
            if (requireType(Boolean.class, visit(ast.getLeft())).equals(false) || requireType(Boolean.class, visit(ast.getRight())).equals(false)) {
                return Environment.create(false);
            }
            else {
                return Environment.create(true);
            }
        }
        else if (ast.getOperator().equals("OR")) {
            if (requireType(Boolean.class, visit(ast.getLeft())).equals(true) || requireType(Boolean.class, visit(ast.getRight())).equals(true)) {
                return Environment.create(true);
            }
            else {
                return Environment.create(false);
            }
        }

        if (ast.getOperator().equals("<")) {
            if ( requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) <= -1 ) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }
        }
        if (ast.getOperator().equals("<=")) {
            if ( requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) <= 0 ) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }
        }
        else if (ast.getOperator().equals(">")) {
            if ( requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) >= 1 ) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }
        } else if (ast.getOperator().equals(">=")) {
            if ( requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) >= 0 ) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }
        }

        if (ast.getOperator().equals("==")) {
            if ( requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) == 0 ) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }
        } else if (ast.getOperator().equals("!=")) {
            if ( requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) != 0 ) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }
        }

        if (ast.getOperator().equals("+")) {
            Environment.PlcObject left = visit(ast.getLeft()), right = visit(ast.getRight());
            //Concatenate
            if (left.getValue().getClass() == String.class) {
                return Environment.create(requireType(String.class, left) + right.getValue());
            } else if (right.getValue().getClass() == String.class) {
                return Environment.create(left.getValue() + requireType(String.class, right));
            }

            //Add
            if (left.getValue().getClass() == BigInteger.class || right.getValue().getClass() == BigInteger.class) {
                return Environment.create(requireType(BigInteger.class, left).add(requireType(BigInteger.class, right)));
            } else if (left.getValue().getClass() == BigDecimal.class || right.getValue().getClass() == BigDecimal.class) {
                return Environment.create(requireType(BigDecimal.class, left).add(requireType(BigDecimal.class, right)));
            }

            throw new RuntimeException("Expected String, BigInteger, or BigDecimal. Got " + left.getValue().getClass() + " and " + right.getValue().getClass());
        }

        if (ast.getOperator().equals("-")) {
            Environment.PlcObject left = visit(ast.getLeft()), right = visit(ast.getRight());
            if (left.getValue().getClass() == BigInteger.class || right.getValue().getClass() == BigInteger.class) {
                return Environment.create(requireType(BigInteger.class, left).subtract(requireType(BigInteger.class, right)));
            } else if (left.getValue().getClass() == BigDecimal.class || right.getValue().getClass() == BigDecimal.class) {
                return Environment.create(requireType(BigDecimal.class, left).subtract(requireType(BigDecimal.class, right)));
            }

            throw new RuntimeException("Expected BigInteger or BigDecimal. Got " + left.getValue().getClass() + " and " + right.getValue().getClass());
        }

        if (ast.getOperator().equals("*")) {
            Environment.PlcObject left = visit(ast.getLeft()), right = visit(ast.getRight());
            if (left.getValue().getClass() == BigInteger.class || right.getValue().getClass() == BigInteger.class) {
                return Environment.create(requireType(BigInteger.class, left).multiply(requireType(BigInteger.class, right)));
            } else if (left.getValue().getClass() == BigDecimal.class || right.getValue().getClass() == BigDecimal.class) {
                return Environment.create(requireType(BigDecimal.class, left).multiply(requireType(BigDecimal.class, right)));
            }

            throw new RuntimeException("Expected BigInteger or BigDecimal. Got " + left.getValue().getClass() + " and " + right.getValue().getClass());
        }

        if (ast.getOperator().equals("/")) {
            Environment.PlcObject left = visit(ast.getLeft()), right = visit(ast.getRight());
            if (left.getValue().getClass() == BigInteger.class) {
                return Environment.create(requireType(BigInteger.class, left).divide(requireType(BigInteger.class, right)));
            } else if (left.getValue().getClass() == BigDecimal.class) {
                return Environment.create(requireType(BigDecimal.class, left).divide(requireType(BigDecimal.class, right), BigDecimal.ROUND_HALF_UP));
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        List<Environment.PlcObject> argObjects = new ArrayList<Environment.PlcObject>();
        List<Ast.Expr> args = ast.getArguments();
        for (int i = 0; i < args.size(); ++i) {
            argObjects.add(visit(args.get(i)));
        }

        if ( ast.getReceiver().isPresent() ) {
            Environment.Variable object = scope.lookupVariable( ((Ast.Expr.Access)ast.getReceiver().get()).getName() );
            return object.getValue().callMethod( ast.getName(), argObjects );
        }

        Environment.Function function = scope.lookupFunction( ast.getName(), ast.getArguments().size() );
        return function.invoke( argObjects );
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
