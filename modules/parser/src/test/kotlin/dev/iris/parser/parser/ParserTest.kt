package dev.iris.parser.parser

import dev.iris.core.ast.Expr
import dev.iris.core.ast.Operator
import dev.iris.core.ast.Stmt
import dev.iris.parser.lexer.Token
import dev.iris.parser.lexer.TokenKind
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun sumOneAndOne() {
        // 1 + 1
        val tokens = listOf(
            Token(TokenKind.INT_LITERAL, "1"),
            Token(TokenKind.PLUS, "+"),
            Token(TokenKind.INT_LITERAL, "1"),
            Token(TokenKind.EOF, ""),
        )
        val expected = listOf(
            Stmt.ExprStmt(Expr.Binary(
                Expr.IntLiteral(1),
                Operator.PLUS,
                Expr.IntLiteral(1)
            ))
        )

        val parser = Parser(tokens)
        val out = parser.parse()
        assertEquals(expected, out.statements)
    }

    @Test
    fun semiComplex() {
        // (a + 2) * b - 4 * c
        val tokens = listOf(
            Token(TokenKind.INT_LITERAL, "1"),
            Token(TokenKind.PLUS, "+"),
            Token(TokenKind.INT_LITERAL, "1"),
            Token(TokenKind.EOF, ""),
        )
        val expected = listOf(
            Stmt.ExprStmt(Expr.Binary(
                Expr.IntLiteral(1),
                Operator.PLUS,
                Expr.IntLiteral(1)
            ))
        )

        val parser = Parser(tokens)
        val out = parser.parse()
        assertEquals(expected, out.statements)
    }
}