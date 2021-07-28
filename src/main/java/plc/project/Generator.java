package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");

        newline(0);
        ++indent;
        for (Ast.Field field : ast.getFields()) {
            newline(indent);
            visit(field);
        }

        newline(indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");

        for (Ast.Method method : ast.getMethods()) {
            newline(0);
            newline(indent);
            visit(method);
        }

        newline(0);
        newline(--indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        System.out.println(ast.getFunction());
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getJvmName(), "(");

        for (int i = 0; i < ast.getParameters().size(); ++i) {
            if (i != 0) {
                print(", ");
            }
            print(ast.getParameterTypeNames().get(i), " ", ast.getParameters().get(i));
        }
        print(") {");

        ++indent;

        for( Ast.Expr.Stmt stmt : ast.getStatements() ) {
            newline(indent);
            visit(stmt);
        }

        --indent;

        if (ast.getStatements().size() != 0) {
            newline(indent);
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()) {
            print( " = ", ((Ast.Expr.Literal)ast.getValue().get()).getLiteral() );
        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver(), " = ");
        visit(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");

        ++indent;
        for (Ast.Stmt thenStatement : ast.getThenStatements()) {
            newline(indent);
            visit(thenStatement);
        }
        newline(--indent);
        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print (" else {");
            ++indent;
            for (Ast.Stmt elseStatement : ast.getElseStatements()) {
                newline(indent);
                visit(elseStatement);
            }
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (int ", ast.getName(), " : ");
        visit(ast.getValue());
        print(") {");

        ++indent;
        for (Ast.Stmt stmt : ast.getStatements()) {
            newline(indent);
            visit(stmt);
        }
        newline(--indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");

        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for(int i = 0; i < ast.getStatements().size(); ++i) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        switch(ast.getType().getName()) {
            case "String":
                print("\"", ast.getLiteral(), "\"");
                break;
            case "Character":
                print("\'", ast.getLiteral(), "\'");
            case "Integer":
                print(ast.getLiteral());
                break;
            case "Decimal": //TODO
                print();
                break;
            case "Boolean":
                print(ast.getLiteral());
                break;
            default:
                throw new RuntimeException("Error, unsupported literal type: " + ast.getType().getName());
        }


        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        print(" ");
        switch(ast.getOperator()) {
            case "AND":
                print("&&");
                break;
            case "OR":
                print("||");
                break;
            case "+":
                print("+");
                break;
            case "-":
                print("-");
                break;
            case "*":
                print("*");
                break;
            case "/":
                print("/");
                break;
            case "<":
                print("<");
                break;
            case ">":
                print(">");
                break;
            case "<=":
                print("<=");
                break;
            case ">=":
                print(">=");
                break;
            case "==":
                print("==");
                break;
            case "!=":
                print("!=");
                break;
            default:
                throw new RuntimeException("Got invalid binary operator. Got: " + ast.getOperator());
        }
        print(" ");
        visit(ast.getRight());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }

        print(ast.getVariable().getJvmName());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getFunction().getJvmName(), "(");

        for (int i = 0; i < ast.getArguments().size(); ++i) {
            if (i != 0) {
                print(", ");
            }
            visit(ast.getArguments().get(i));
        }

        print(")");

        return null;
    }

}
