package plc.project;

import java.io.PrintWriter;

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
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
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
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
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
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        return null;
    }

}
