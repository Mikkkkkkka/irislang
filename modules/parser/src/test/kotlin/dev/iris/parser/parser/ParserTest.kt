package dev.iris.parser.parser

import dev.iris.core.Diagnostic
import dev.iris.core.ast.Decl
import dev.iris.core.ast.Expr
import dev.iris.core.ast.Operator
import dev.iris.core.ast.Stmt
import dev.iris.parser.lexer.Token
import dev.iris.parser.lexer.TokenKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun sumOneAndOne() {
        // 1 + 1;
        val tokens = listOf(
            Token(TokenKind.INT_LITERAL, "1"),
            Token(TokenKind.PLUS, "+"),
            Token(TokenKind.INT_LITERAL, "1"),
            Token(TokenKind.SEMICOLON, ";"),
            Token(TokenKind.EOF, ""),
        )
        val expectedDiagnostics = listOf<Diagnostic>()
        val expectedDeclarations = listOf<Decl>()
        val expectedStatements = listOf(
            Stmt.ExprStmt(Expr.Binary(
                Expr.IntLiteral(1),
                Operator.ADD,
                Expr.IntLiteral(1)
            ))
        )

        val parser = Parser(tokens)
        val out = parser.parse()
        assertNotNull(out.program)
        assertEquals(expectedDiagnostics, out.diagnostics)
        assertEquals(expectedDeclarations, out.program.declarations)
        assertEquals(expectedStatements, out.program.statements)
    }

    @Test
    fun functionCall() {
        // some_func();
        val tokens = listOf(
            Token(TokenKind.IDENT, "some_func"),
            Token(TokenKind.LPAREN, "("),
            Token(TokenKind.RPAREN, ")"),
            Token(TokenKind.SEMICOLON, ";"),
            Token(TokenKind.EOF, ""),
        )
        val expectedDiagnostics = listOf<Diagnostic>()
        val expectedDeclarations = listOf<Decl>()
        val expectedStatements = listOf(
            Stmt.ExprStmt(Expr.Call(
                "some_func",
                listOf()
            ))
        )

        val parser = Parser(tokens)
        val out = parser.parse()
        assertNotNull(out.program)
        assertEquals(expectedDiagnostics, out.diagnostics)
        assertEquals(expectedDeclarations, out.program.declarations)
        assertEquals(expectedStatements, out.program.statements)
    }

    @Test
    fun functionCallWithArgs() {
        // some_func(a, b);
        val tokens = listOf(
            Token(TokenKind.IDENT, "some_func"),
            Token(TokenKind.LPAREN, "("),
            Token(TokenKind.IDENT, "a"),
            Token(TokenKind.COMMA, ","),
            Token(TokenKind.IDENT, "b"),
            Token(TokenKind.RPAREN, ")"),
            Token(TokenKind.SEMICOLON, ";"),
            Token(TokenKind.EOF, ""),
        )
        val expectedDiagnostics = listOf<Diagnostic>()
        val expectedDeclarations = listOf<Decl>()
        val expectedStatements = listOf(
            Stmt.ExprStmt(Expr.Call(
                "some_func",
                listOf(
                    Expr.Var("a"),
                    Expr.Var("b"),
                )
            ))
        )

        val parser = Parser(tokens)
        val out = parser.parse()
        assertNotNull(out.program)
        assertEquals(expectedDiagnostics, out.diagnostics)
        assertEquals(expectedDeclarations, out.program.declarations)
        assertEquals(expectedStatements, out.program.statements)
    }

    @Test
    fun functionCallWithExprArgs() {
        // some_func(a * (b + c), (1 + 2) * 4);
        val tokens = listOf(
            Token(TokenKind.IDENT, "some_func"),
            Token(TokenKind.LPAREN, "("),

            Token(TokenKind.IDENT, "a"),
            Token(TokenKind.STAR, "*"),
            Token(TokenKind.LPAREN, "("),
            Token(TokenKind.IDENT, "b"),
            Token(TokenKind.PLUS, "+"),
            Token(TokenKind.IDENT, "c"),
            Token(TokenKind.RPAREN, ")"),

            Token(TokenKind.COMMA, ","),

            Token(TokenKind.LPAREN, "("),
            Token(TokenKind.INT_LITERAL, "1"),
            Token(TokenKind.PLUS, "+"),
            Token(TokenKind.INT_LITERAL, "2"),
            Token(TokenKind.RPAREN, ")"),
            Token(TokenKind.STAR, "*"),
            Token(TokenKind.INT_LITERAL, "4"),

            Token(TokenKind.RPAREN, ")"),
            Token(TokenKind.SEMICOLON, ";"),

            Token(TokenKind.EOF, ""),
        )
        val expectedDiagnostics = listOf<Diagnostic>()
        val expectedDeclarations = listOf<Decl>()
        val expectedStatements = listOf(
            Stmt.ExprStmt(Expr.Call(
                "some_func",
                listOf(
                    Expr.Binary(
                        left = Expr.Var("a"),
                        operator = Operator.MULTIPLY,
                        right = Expr.Binary(
                            left = Expr.Var("b"),
                            operator = Operator.ADD,
                            right = Expr.Var("c"),
                        )
                    ),
                    Expr.Binary(
                        left = Expr.Binary(
                            left = Expr.IntLiteral(1),
                            operator = Operator.ADD,
                            right = Expr.IntLiteral(2),
                        ),
                        operator = Operator.MULTIPLY,
                        right = Expr.IntLiteral(4),
                    )
                )
            ))
        )

        val parser = Parser(tokens)
        val out = parser.parse()
        assertNotNull(out.program)
        assertEquals(expectedDiagnostics, out.diagnostics)
        assertEquals(expectedDeclarations, out.program.declarations)
        assertEquals(expectedStatements, out.program.statements)
   }

    @Test
    fun semiComplex() {
        // (a + 2) * b - 4 * c;
        val tokens = listOf(
            Token(TokenKind.LPAREN, "("),
            Token(TokenKind.IDENT, "a"),
            Token(TokenKind.PLUS, "+"),
            Token(TokenKind.INT_LITERAL, "2"),
            Token(TokenKind.RPAREN, ")"),
            Token(TokenKind.STAR, "*"),
            Token(TokenKind.IDENT, "b"),
            Token(TokenKind.MINUS, "-"),
            Token(TokenKind.INT_LITERAL, "4"),
            Token(TokenKind.STAR, "*"),
            Token(TokenKind.IDENT, "c"),
            Token(TokenKind.SEMICOLON, ";"),
            Token(TokenKind.EOF, ""),
        )
        val expected = listOf(
            Stmt.ExprStmt(Expr.Binary(
                left = Expr.Binary(
                    left = Expr.Binary(
                        left = Expr.Var("a"),
                        operator = Operator.ADD,
                        right = Expr.IntLiteral(2)
                    ),
                    operator = Operator.MULTIPLY,
                    right = Expr.Var("b")
                ),
                operator = Operator.SUBTRACT,
                right = Expr.Binary(
                    left = Expr.IntLiteral(4),
                    operator = Operator.MULTIPLY,
                    right = Expr.Var("c")
                )
            ))
        )

        val parser = Parser(tokens)
        val out = parser.parse()
        assertNotNull(out.program)
        assertTrue(out.diagnostics.isEmpty())
        assertEquals(expected, out.program.statements)
    }
}