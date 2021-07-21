package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Optional;

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
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        throw new UnsupportedOperationException();  // TODOs
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
        if (ast.getReceiver().getClass() != Ast.Expr.Access.class) {
            throw new RuntimeException("Error: Receiver must be an access expression. Got: " + ast.getReceiver().getClass());
        }

        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        String bool = String.valueOf(ast.getCondition());
        boolean tf = (bool.contains("true") || bool.contains("false"));
        if ( ast.getThenStatements().isEmpty() || !tf) {
            throw new RuntimeException("Invalid If Statement.");
        }
        else if (tf) {
            ast.getThenStatements().forEach(this::visit);
            ast.getElseStatements().forEach(this::visit);
        }
        //requireAssignable(Environment.getType(), Environment.Type.BOOLEAN);



        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        if ( ast.getStatements().isEmpty() || (ast.getValue().getType() != Environment.Type.INTEGER_ITERABLE)) {
            throw new RuntimeException("Invalid For Statement.");
        }

        for (Object element : ast.getStatements()) {
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(), (Environment.PlcObject) element);
            throw new UnsupportedOperationException();  // TODO
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        Object value = ast.getLiteral();

        if (value instanceof BigInteger) {
            long intValue = ((BigInteger) value).longValue();
            if ((intValue >= -2147483648) && (intValue <= 2147483647)) {
                ast.setType(Environment.Type.INTEGER);
            }
            else {
                throw new RuntimeException("Value is out of range of a Java int (32-bit signed int)");
            }
        }
        else if (value instanceof BigDecimal) {
            long valLong = ((BigDecimal) value).longValue();
            if ((valLong >= -9223372036854775808L) && (valLong <= 9223372036854775807L)) {
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
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        Ast.Expr left = ast.getLeft();
        Ast.Expr right = ast.getRight();
        String opr = ast.getOperator();
        //left.toString().contains("true") || left.toString().contains("false")) && (right.toString().contains("true") || right.toString().contains("false")
        if (opr.equals("AND") || opr.equals("OR")) {
            if (left.getType() == Environment.Type.BOOLEAN && right.getType() == Environment.Type.BOOLEAN) {
                ast.setType(Environment.Type.BOOLEAN);
            }
        }
        else if (opr.equals("<") || opr.equals("<=") || opr.equals(">") || opr.equals(">=") || opr.equals("==") || opr.equals("!=")) {
            if (left.getType() == Environment.Type.COMPARABLE && right.getType() == Environment.Type.COMPARABLE) {
                ast.setType(Environment.Type.BOOLEAN);
            }
        }
        else if (opr.equals("+")) {
            if (left.getType() == Environment.Type.STRING && right.getType() == Environment.Type.STRING) {
                ast.setType(Environment.Type.STRING);
            }
            else if (left.getType() == Environment.Type.INTEGER && right.getType() == Environment.Type.INTEGER) {
                ast.setType(Environment.Type.INTEGER);
            }
            else if (left.getType() == Environment.Type.DECIMAL && right.getType() == Environment.Type.DECIMAL) {
                ast.setType(Environment.Type.DECIMAL);
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {

        if (ast.getReceiver().isPresent()) {
//            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), ast.getReceiver().get().getType(), Environment.NIL));
            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL));
        }
        else {
            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), scope.lookupVariable(ast.getName()).getType(), Environment.NIL));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
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
                if (tempType.contains("Integer")){ break; }
                else if (tempType.contains("Decimal")){ break; }
                else if (tempType.contains("Character")){ break; }
                else if (tempType.contains("String")){ break; }
                else { throw new RuntimeException("Target of Type 'Comparable' did not match anything from our language can be assigned to it."); }
            case "Integer":
                if (tempType.contains("Integer")){ break; }
                throw new RuntimeException("Target of Type 'Integer' did not match Type: " + tempType);
            case "Decimal":
                if (tempType.contains("Decimal")){ break; }
                throw new RuntimeException("Target of Type 'Decimal' did not match Type: " + tempType);
            case "Character":
                if (tempType.contains("Character")){ break; }
                throw new RuntimeException("Target of Type 'Character' did not match Type: " + tempType);
            case "String":
                if (tempType.contains("String")){ break; }
                throw new RuntimeException("Target of Type 'String' did not match Type: " + tempType);
            default:
                throw new RuntimeException("The target type could not be assigned to any type.");
        }
    }

}
