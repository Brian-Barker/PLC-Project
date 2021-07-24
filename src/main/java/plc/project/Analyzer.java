package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        System.out.println(ast.getFields());
        ast.getFields().forEach(this::visit);

        Boolean gotMain = false;
        for (Ast.Method func : ast.getMethods()) {
            if (func.getName().equals("main") && func.getParameters().size() == 0) {
                if (gotMain) {
                    throw new RuntimeException("Error: multiple \"main\" functions received.");
                }
                gotMain = true;
                if (!func.getReturnTypeName().isPresent()) throw new RuntimeException("Error: no return type for main. Should return Integer.");
                requireAssignable( Environment.Type.INTEGER, Environment.getType(func.getReturnTypeName().get()) );
            }
            visit(func);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            requireAssignable( Environment.getType(ast.getTypeName()), ast.getValue().get().getType() );
            visit(ast.getValue().get());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> parameterTypes = new ArrayList<>();
        for (String typeName : ast.getParameterTypeNames()) {
            parameterTypes.add(Environment.getType(typeName));
        }

        Environment.Type returnType;
        if (ast.getReturnTypeName().isPresent())
            returnType = Environment.getType(ast.getReturnTypeName().get());
        else
            returnType = Environment.Type.NIL;

        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
        }

        java.util.function.Function<List<Environment.PlcObject>, Environment.PlcObject> function = arguments -> { return Environment.NIL; };
        ast.setFunction(scope.defineFunction( ast.getName(), ast.getName(), parameterTypes, returnType, function));

        scope = new Scope(scope);
        scope.defineVariable("$RETURN_TYPE", Environment.create(ast.getReturnTypeName().get()));

        for (int i = 0; i < ast.getParameters().size(); ++i) {
            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), Environment.getType(ast.getParameterTypeNames().get(i)), Environment.NIL);
        }
        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
        }
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());

        if (ast.getExpression().getClass() != Ast.Expr.Function.class)
            throw new RuntimeException("Error: Expression not a Ast.Expr.Function. Got: " + ast.getExpression().getClass());

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Declaration must have type or value to infer type.");
        }

        Environment.Type type = null;

        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            // if (!ast.getTypeName().isPresent())
            if (type == null) {
                type = ast.getValue().get().getType();
            }

            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        visit(ast.getReceiver());
        visit(ast.getValue());

        if (ast.getReceiver().getClass() != Ast.Expr.Access.class) {
            throw new RuntimeException("Error: Receiver must be an access expression. Got: " + ast.getReceiver().getClass());
        }

        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        if ( ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Invalid If Statement.");
        }
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        ast.getThenStatements().forEach(this::visit);
        ast.getElseStatements().forEach(this::visit);

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());
        if (ast.getStatements().size() == 0) {
            throw new RuntimeException("Error: Statement List empty");
        }

        for (Ast.Stmt stmt : ast.getStatements()) {
            scope = new Scope(scope);

            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            visit(stmt);

            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        for (Ast.Stmt stmt : ast.getStatements()) {
            scope = new Scope(scope);

            visit(stmt);

            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        visit(ast.getValue());

        requireAssignable(scope.lookupVariable("$Return_Type").getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        Object value = ast.getLiteral();

        if (value instanceof BigInteger) {
            if ( ((BigInteger) value).bitLength() <= 32 ) {
                ast.setType(Environment.Type.INTEGER);
            }
            else {
                throw new RuntimeException("Value is out of range of a Java int (32-bit signed int)");
            }
        }
        else if (value instanceof BigDecimal) {
            if ( ((BigDecimal) value).doubleValue() != Double.POSITIVE_INFINITY && ((BigDecimal) value).doubleValue() != Double.NEGATIVE_INFINITY ) {
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("Value is out of range of a Java int (32-bit signed int)");
            }
        }
        else if (value instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
            return null;
        }
        else if (value instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
            return null;
        }
        else if (value instanceof String) {
            ast.setType(Environment.Type.STRING);
            return null;
        } else {
            ast.setType(Environment.Type.NIL);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        visit(ast.getExpression());

        if (ast.getExpression().getClass() != Ast.Expr.Binary.class) {
            throw new RuntimeException("Error: Group must contain binary expression. Got: " + ast.getExpression().getClass());
        }

        ast.setType(ast.getExpression().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        Ast.Expr left = ast.getLeft();
        visit(left);
        Ast.Expr right = ast.getRight();
        visit(right);
        String opr = ast.getOperator();

        requireAssignable(Environment.Type.ANY, left.getType());
        requireAssignable(Environment.Type.ANY, right.getType());

        if (opr.equals("AND") || opr.equals("OR")) {
            requireAssignable(Environment.Type.BOOLEAN, left.getType());
            requireAssignable(Environment.Type.BOOLEAN, right.getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (opr.equals("<") || opr.equals("<=") || opr.equals(">") || opr.equals(">=") || opr.equals("==") || opr.equals("!=")) {
            requireAssignable(Environment.Type.COMPARABLE, left.getType());
            if (!left.getType().equals(right.getType())) {
                throw new RuntimeException("Error: type of left and right must be the same. Got: " + left.getType() + " and " + right.getType());
            }
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (opr.equals("+")) {
            if (left.getType() == Environment.Type.STRING || right.getType() == Environment.Type.STRING) {
                ast.setType(Environment.Type.STRING);
            }
            else if (left.getType() == Environment.Type.INTEGER) {
                requireAssignable(Environment.Type.INTEGER, right.getType());
                ast.setType(Environment.Type.INTEGER);
            }
            else if (left.getType() == Environment.Type.DECIMAL) {
                requireAssignable(Environment.Type.DECIMAL, right.getType());
                ast.setType(Environment.Type.DECIMAL);
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), ast.getReceiver().get().getType().getField(ast.getName()).getType(), Environment.NIL));
        } else {
            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), scope.lookupVariable(ast.getName()).getType(), Environment.NIL));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        Environment.Function function;

        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            function = ast.getReceiver().get().getType().getMethod(ast.getName(), ast.getArguments().size());

            for (int i = 1; i < ast.getArguments().size(); ++i) {
                visit(ast.getArguments().get(i));
                requireAssignable(function.getParameterTypes().get(i-1), ast.getArguments().get(i).getType());
            }
        } else {
            function = scope.lookupFunction(ast.getName(), ast.getArguments().size());

            for (int i = 0; i < ast.getArguments().size(); ++i) {
                visit(ast.getArguments().get(i));
                requireAssignable(function.getParameterTypes().get(i), ast.getArguments().get(i).getType());
            }
        }

        ast.setFunction(function);

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        String tempType = type.getName();
        String tempTarget = target.getName();

        System.out.println("Type: " + type.getName());
        System.out.println("Target: " + target.getName());

        switch (tempTarget) {
            case "Any":
                break;
            case "Comparable":
                if (tempType.equals("Integer")){ break; }
                else if (tempType.contains("Decimal")){ break; }
                else if (tempType.contains("Character")){ break; }
                else if (tempType.contains("String")){ break; }
                else { throw new RuntimeException("Target of Type 'Comparable' did not match anything from our language can be assigned to it."); }
            case "Integer":
                if (tempType.equals("Integer")){ break; }
                throw new RuntimeException("Target of Type 'Integer' did not match Type: " + tempType);
            case "Decimal":
                if (tempType.equals("Decimal")){ break; }
                throw new RuntimeException("Target of Type 'Decimal' did not match Type: " + tempType);
            case "Character":
                if (tempType.equals("Character")){ break; }
                throw new RuntimeException("Target of Type 'Character' did not match Type: " + tempType);
            case "String":
                if (tempType.equals("String")){ break; }
                throw new RuntimeException("Target of Type 'String' did not match Type: " + tempType);
            case "Boolean":
                if (tempType.equals("Boolean")){ break; }
                throw new RuntimeException("Target of Type 'Boolean' did not match Type: " + tempType);
            default:
                throw new RuntimeException("The target type could not be assigned to any type.");
        }
    }

}
