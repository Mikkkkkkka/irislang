package dev.iris.parser.lexer

import dev.iris.core.Span

data class Token(
    val kind: TokenKind,
    val lexeme: String,
    val span: Span? = null,
) {
    override fun toString() = "Token{kind:$kind lexeme:'$lexeme' span:$span}"
}