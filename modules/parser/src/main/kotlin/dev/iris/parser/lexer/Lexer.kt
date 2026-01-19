package dev.iris.parser.lexer

import dev.iris.core.Diagnostic
import dev.iris.core.Span

class Lexer(
    private val text: String
) {
    private var pos = 0
    private val tokens = mutableListOf<Token>()
    private val diagnostics = mutableListOf<Diagnostic>()

    fun tokenize(): LexResult {

        while (!isAtEnd()) {
            val c = text[pos]

            if (c.isWhitespace()) {
                pos++
                continue
            }

            // Комментарии
            if (c == '/' && pos + 1 < text.length && text[pos + 1] == '/') {
                while (!isAtEnd() && text[pos] != '\n') pos++
                continue
            }

            val start = pos

            if (c.isDigit()) {
                while (!isAtEnd() && text[pos].isDigit()) pos++
                tokens.add(makeToken(TokenKind.INT_LITERAL, start, pos))
                continue
            }

            if (c.isLetter() || c == '_') {
                while (!isAtEnd() && (text[pos].isLetterOrDigit() || text[pos] == '_')) pos++
                val lexeme = text.substring(start, pos)
                val kind = KEYWORDS[lexeme] ?: TokenKind.IDENT
                tokens.add(Token(kind, lexeme, Span(start, pos)))
                continue
            }

            when (c) {
                '+' -> tokens.add(makeToken(TokenKind.PLUS, start, ++pos))
                '-' -> tokens.add(makeToken(TokenKind.MINUS, start, ++pos))
                '*' -> tokens.add(makeToken(TokenKind.STAR, start, ++pos))
                '/' -> tokens.add(makeToken(TokenKind.SLASH, start, ++pos))
                '%' -> tokens.add(makeToken(TokenKind.PERCENT, start, ++pos))
                '(' -> tokens.add(makeToken(TokenKind.LPAREN, start, ++pos))
                ')' -> tokens.add(makeToken(TokenKind.RPAREN, start, ++pos))
                ',' -> tokens.add(makeToken(TokenKind.COMMA, start, ++pos))
                '.' -> tokens.add(makeToken(TokenKind.DOT, start, ++pos))
                ':' -> tokens.add(makeToken(TokenKind.COLON, start, ++pos))
                ';' -> tokens.add(makeToken(TokenKind.SEMICOLON, start, ++pos))
                '?' -> tokens.add(makeToken(TokenKind.QUESTION, start, ++pos))
                '[' -> tokens.add(makeToken(TokenKind.LBRACKET, start, ++pos))
                ']' -> tokens.add(makeToken(TokenKind.RBRACKET, start, ++pos))

                '=' -> {
                    pos++
                    if (!isAtEnd() && text[pos] == '=') {
                        pos++
                        tokens.add(makeToken(TokenKind.EQEQ, start, pos))
                    } else {
                        tokens.add(makeToken(TokenKind.EQ, start, pos))
                    }
                }

                '!' -> {
                    pos++
                    if (!isAtEnd() && text[pos] == '=') {
                        pos++
                        tokens.add(makeToken(TokenKind.NEQ, start, pos))
                    } else {
                        tokens.add(makeToken(TokenKind.BANG, start, pos))
                    }
                }

                '<' -> {
                    pos++
                    if (!isAtEnd() && text[pos] == '=') {
                        pos++
                        tokens.add(makeToken(TokenKind.LTEQ, start, pos))
                    } else {
                        tokens.add(makeToken(TokenKind.LT, start, pos))
                    }
                }

                '>' -> {
                    pos++
                    if (!isAtEnd() && text[pos] == '=') {
                        pos++
                        tokens.add(makeToken(TokenKind.GTEQ, start, pos))
                    } else {
                        tokens.add(makeToken(TokenKind.GT, start, pos))
                    }
                }

                else -> {
                    tokens.add(makeError("Unexpected symbol: $c", start, pos))
                    pos++
                }
            }
        }

        tokens.add(makeToken(TokenKind.EOF, pos, pos))
        return LexResult(tokens, diagnostics)
    }

    private fun isAtEnd(): Boolean = pos >= text.length

    private fun makeToken(kind: TokenKind, start: Int, end: Int) =
        Token(kind, text.substring(start, end), Span(start, end))

    private fun makeError(message: String, start: Int, end: Int): Token {
        val diag = Diagnostic("Lexer Error: '$message'", Span(start, pos))
        diagnostics.add(diag)
        return Token(TokenKind.ERROR, text.substring(start, end), Span(start, end))
    }

    companion object {
        private val KEYWORDS = mapOf(
            "целч" to TokenKind.KW_INT,
            "бул" to TokenKind.KW_BOOL,
            "если" to TokenKind.KW_IF,
            "иначе" to TokenKind.KW_ELSE,
            "пока" to TokenKind.KW_WHILE,
            "для" to TokenKind.KW_FOR,
            "от" to TokenKind.KW_FROM,
            "до" to TokenKind.KW_TO,
            "нч" to TokenKind.KW_START,
            "кц" to TokenKind.KW_END,
            "функция" to TokenKind.KW_FUNCTION,
            "процедура" to TokenKind.KW_PROCEDURE,
            "структура" to TokenKind.KW_STRUCTURE,
            "вернуть" to TokenKind.KW_RETURN,
            "выйти" to TokenKind.KW_BREAK,
            "продолжить" to TokenKind.KW_CONTINUE,
            "новый" to TokenKind.KW_NEW,
            "печать" to TokenKind.KW_PRINT,
            "правда" to TokenKind.KW_TRUE,
            "ложь" to TokenKind.KW_FALSE,
            "и" to TokenKind.KW_AND,
            "или" to TokenKind.KW_OR,
            "нуль" to TokenKind.KW_NULL,
        )
    }
}
