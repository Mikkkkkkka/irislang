package dev.iris.lexer

import dev.iris.core.Span
import kotlin.test.Test
import kotlin.test.assertEquals

class LexerTest {
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