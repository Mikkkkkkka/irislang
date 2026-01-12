package dev.iris.lexer

import dev.iris.core.Span
import kotlin.test.Test
import kotlin.test.assertEquals

class LexerTest {
    @Test
    fun variableDeclaration() {
        val irisCode = "целч а\n"
        val expected = listOf(
            Token(TokenKind.KW_INT, "целч", Span(0, 4)),
            Token(TokenKind.IDENT, "а", Span(5, 6)),
            Token(TokenKind.EOF, "", Span(7, 7)),
        )

        val lexer = Lexer(irisCode)
        val out = lexer.tokenize()
        assertEquals(expected, out.tokens)
    }

    @Test
    fun variableAssignment() {
        val irisCode = "а = 10\n"
        val expected = listOf(
            Token(TokenKind.IDENT, "а", Span(0, 1)),
            Token(TokenKind.EQ, "=", Span(2, 3)),
            Token(TokenKind.INT_LITERAL, "10", Span(4, 6)),
            Token(TokenKind.EOF, "", Span(7, 7)),
        )

        val lexer = Lexer(irisCode)
        val out = lexer.tokenize()
        assertEquals(expected, out.tokens)
    }

    @Test
    fun operators() {
        val irisCode = "1 + 2 - 3 * 4 / 5 % 6\n"
        val expected = listOf(
            Token(TokenKind.INT_LITERAL, "1", Span(0, 1)),
            Token(TokenKind.PLUS, "+", Span(2, 3)),
            Token(TokenKind.INT_LITERAL, "2", Span(4, 5)),
            Token(TokenKind.MINUS, "-", Span(6, 7)),
            Token(TokenKind.INT_LITERAL, "3", Span(8, 9)),
            Token(TokenKind.STAR, "*", Span(10, 11)),
            Token(TokenKind.INT_LITERAL, "4", Span(12, 13)),
            Token(TokenKind.SLASH, "/", Span(14, 15)),
            Token(TokenKind.INT_LITERAL, "5", Span(16, 17)),
            Token(TokenKind.PERCENT, "%", Span(18, 19)),
            Token(TokenKind.INT_LITERAL, "6", Span(20, 21)),
            Token(TokenKind.EOF, "", Span(22, 22)),
        )

        val lexer = Lexer(irisCode)
        val out = lexer.tokenize()
        assertEquals(expected, out.tokens)
    }

    @Test
    fun ifElseBlocks() {
        val irisCode = "если (правда) нч результат = 1 кц\n"
        val expected = listOf(
            Token(TokenKind.KW_IF, "если", Span(0, 4)),
            Token(TokenKind.LPAREN, "(", Span(5, 6)),
            Token(TokenKind.KW_TRUE, "правда", Span(6, 12)),
            Token(TokenKind.RPAREN, ")", Span(12, 13)),
            Token(TokenKind.KW_START, "нч", Span(14, 16)),
            Token(TokenKind.IDENT, "результат", Span(17, 26)),
            Token(TokenKind.EQ, "=", Span(27, 28)),
            Token(TokenKind.INT_LITERAL, "1", Span(29, 30)),
            Token(TokenKind.KW_END, "кц", Span(31, 33)),
            Token(TokenKind.EOF, "", Span(34, 34)),
        )

        val lexer = Lexer(irisCode)
        val out = lexer.tokenize()
        assertEquals(expected, out.tokens)
    }

    @Test
    fun whileBlock() {
        val irisCode = "пока (ложь) нч результат = результат + 1 кц\n"
        val expected = listOf(
            Token(TokenKind.KW_WHILE, "пока", Span(0, 4)),
            Token(TokenKind.LPAREN, "(", Span(5, 6)),
            Token(TokenKind.KW_FALSE, "ложь", Span(6, 10)),
            Token(TokenKind.RPAREN, ")", Span(10, 11)),
            Token(TokenKind.KW_START, "нч", Span(12, 14)),
            Token(TokenKind.IDENT, "результат", Span(15, 24)),
            Token(TokenKind.EQ, "=", Span(25, 26)),
            Token(TokenKind.IDENT, "результат", Span(27, 36)),
            Token(TokenKind.PLUS, "+", Span(37, 38)),
            Token(TokenKind.INT_LITERAL, "1", Span(39, 40)),
            Token(TokenKind.KW_END, "кц", Span(41, 43)),
            Token(TokenKind.EOF, "", Span(44, 44)),
        )

        val lexer = Lexer(irisCode)
        val out = lexer.tokenize()
        assertEquals(expected, out.tokens)
    }

    @Test
    fun printing() {
        val irisCode = "печать:(1)\n"
        val expected = listOf(
            Token(TokenKind.KW_PRINT, "печать", Span(0, 6)),
            Token(TokenKind.COLON, ":", Span(6, 7)),
            Token(TokenKind.LPAREN, "(", Span(7, 8)),
            Token(TokenKind.INT_LITERAL, "1", Span(8, 9)),
            Token(TokenKind.RPAREN, ")", Span(9, 10)),
            Token(TokenKind.EOF, "", Span(11, 11)),
        )

        val lexer = Lexer(irisCode)
        val out = lexer.tokenize()
        assertEquals(expected, out.tokens)
    }
}