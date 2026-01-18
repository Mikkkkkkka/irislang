package dev.iris.parser.parser

import dev.iris.core.Diagnostic
import dev.iris.core.ast.Decl
import dev.iris.core.ast.Expr
import dev.iris.core.ast.Operator
import dev.iris.core.ast.Program
import dev.iris.core.ast.Stmt
import dev.iris.core.ast.TypeRef
import dev.iris.parser.lexer.Token
import dev.iris.parser.lexer.TokenKind

class Parser(
    private val tokens: List<Token>,
) {
    private var pos = 0
    private val diagnostics = mutableListOf<Diagnostic>()

    fun parse(): Program {
        val declarations = mutableListOf<Decl>()
        val statements = mutableListOf<Stmt>()

        while (!isAtEnd()) {
            when (peek().kind) {
                TokenKind.KW_STRUCT -> declarations += parseStruct()
                TokenKind.KW_FUNCTION -> declarations += parseFunction()
                TokenKind.KW_PROCEDURE -> declarations += parseProcedure()
                else -> statements += parseStmt()
            }
        }

        return Program(declarations, statements)
    }

    // Декларации
    private fun parseStruct(): Decl.Structure {
        val name = expect(TokenKind.IDENT, "Имя структуры").lexeme
        expect(TokenKind.KW_START, "Ожидалось 'нч'")

        val fields = mutableListOf<Stmt.VarDecl>()
        while (!check(TokenKind.KW_END)) {
            val fieldStart = peek().span
            val fieldType = parseType()
            val fieldName = expect(TokenKind.IDENT, "Имя поля").lexeme
            fields += Stmt.VarDecl(fieldType, fieldName, null,fieldStart)
        }

        expect(TokenKind.KW_END, "Ожидалось 'кц'")
        return Decl.Structure(name, fields)
    }

    private fun parseProcedure(): Decl.Procedure {
        val name = expect(TokenKind.IDENT, "Имя процедуры").lexeme
        val params = parseParams()
        val body = parseBlock()
        return Decl.Procedure(name, params, body)
    }

    private fun parseFunction(): Decl.Function {
        expect(TokenKind.COLON, "Ожидался ':'")
        val returnType = parseType()
        val name = expect(TokenKind.IDENT, "Имя функции").lexeme
        val params = parseParams()
        val body = parseBlock()
        return Decl.Function(name, returnType, params, body)
    }

    private fun parseParams(): List<Stmt.VarDecl> {
        expect(TokenKind.LPAREN, "Ожидалась '('")
        val params = mutableListOf<Stmt.VarDecl>()

        if (!check(TokenKind.RPAREN)) {
            do {
                val start = peek().span
                val type = parseType()
                val name = expect(TokenKind.IDENT, "Имя параметра").lexeme
                params += Stmt.VarDecl(type, name, null,start)
            } while (match(TokenKind.COMMA))
        }

        expect(TokenKind.RPAREN, "Ожидалась ')'")
        return params
    }

    // Утверждения (statements)
    private fun parseStmt(): Stmt {
        return when {
            match(TokenKind.KW_IF) -> parseIf()
            match(TokenKind.KW_WHILE) -> parseWhile()
            match(TokenKind.KW_RETURN) -> Stmt.Return(parseExpr())
            match(TokenKind.KW_BREAK) -> Stmt.Break()
            match(TokenKind.KW_CONTINUE) -> Stmt.Continue()
            match(TokenKind.KW_START) -> parseBlockInside()
            isTypeStart(peek().kind) -> parseVarDecl()
            else -> Stmt.ExprStmt(parseExpr())
        }
    }

    private fun parseBlock(): Stmt.Block {
        expect(TokenKind.KW_START, "Ожидалось 'нч'")
        return parseBlockInside()
    }

    private fun parseBlockInside(): Stmt.Block {
        val stmts = mutableListOf<Stmt>()
        while (!check(TokenKind.KW_END)) {
            stmts += parseStmt()
        }
        expect(TokenKind.KW_END, "Ожидалось 'кц'")
        return Stmt.Block(stmts)
    }

    private fun parseIf(): Stmt.If {
        expect(TokenKind.LPAREN, "Ожидалась '('")
        val cond = parseExpr()
        expect(TokenKind.RPAREN, "Ожидалась ')'")

        val thenBlock = parseBlock()
        var elseBranch: Stmt? = null

        if (match(TokenKind.KW_ELSE)) {
            elseBranch =
                if (match(TokenKind.KW_IF)) parseIf()
                else parseBlock()
        }

        return Stmt.If(cond, thenBlock, elseBranch)
    }

    private fun parseWhile(): Stmt.While {
        expect(TokenKind.LPAREN, "Ожидалась '('")
        val cond = parseExpr()
        expect(TokenKind.RPAREN, "Ожидалась ')'")
        val body = parseBlock()
        return Stmt.While(cond, body)
    }

    private fun parseVarDecl(): Stmt.VarDecl {
        val type = parseType()
        val name = expect(TokenKind.IDENT, "Имя переменной").lexeme
        val init = if (match(TokenKind.EQ)) parseExpr() else null
        return Stmt.VarDecl(type, name, init)
    }

    // Типы
    private fun parseType(): TypeRef {
        val base = when {
            match(TokenKind.KW_INT) -> TypeRef.IntType
            match(TokenKind.KW_BOOL) -> TypeRef.BoolType
            match(TokenKind.IDENT) -> TypeRef.StructType(previous().lexeme)
            else -> error("Ожидался тип")
        }

        var t = base

        while (match(TokenKind.STAR)) {
            t = TypeRef.Pointer(t)
        }

        if (match(TokenKind.COLON)) {
            val size = expect(TokenKind.INT_LITERAL, "Ожидалась числовая константа").lexeme.toULong()
            t = TypeRef.Array(t, size)
        }

        return t
    }

    // Выражения
    private fun parseExpr(): Expr = parseEquality()

    private fun parseEquality(): Expr {
        var expr = parseAdd()

        while (match(TokenKind.EQEQ, TokenKind.NEQ)) {
            val op = previous().kind
            val right = parseAdd()
            expr = Expr.Binary(expr, mapTokenTypeToOperator(op), right)
        }

        return expr
    }

    private fun parseAdd(): Expr {
        var expr = parseMul()

        while (match(TokenKind.PLUS, TokenKind.MINUS)) {
            val op = previous().kind
            val right = parseMul()
            expr = Expr.Binary(expr, mapTokenTypeToOperator(op), right)
        }

        return expr
    }

    private fun parseMul(): Expr {
        var expr = parsePrimary()

        while (match(TokenKind.STAR, TokenKind.SLASH, TokenKind.PERCENT)) {
            val op = previous().kind
            val right = parsePrimary()
            expr = Expr.Binary(expr, mapTokenTypeToOperator(op), right)
        }

        return expr
    }

    private fun parsePrimary(): Expr {
        if (match(TokenKind.INT_LITERAL)) return Expr.IntLiteral(previous().lexeme.toInt())
        if (match(TokenKind.KW_TRUE)) return Expr.BoolLiteral(true)
        if (match(TokenKind.KW_FALSE)) return Expr.BoolLiteral(false)

        if (match(TokenKind.KW_PRINT)) {
            expect(TokenKind.COLON, ":")
            expect(TokenKind.LPAREN, "(")
            val e = parseExpr()
            expect(TokenKind.RPAREN, ")")
            return Expr.Call("печать:", listOf(e))
        }

        if (match(TokenKind.KW_NEW)) {
            expect(TokenKind.COLON, ":")
            val type = parseType()
            val size = if (match(TokenKind.COLON)) parseExpr() else null
            return Expr.New(type, size)
        }

        if (match(TokenKind.IDENT)) {
            return Expr.Var(previous().lexeme)
        }

        if (match(TokenKind.LPAREN)) {
            val e = parseExpr()
            expect(TokenKind.RPAREN, ")")
            return e
        }

        error("Ожидалось выражение")
    }

    // Вспомогательные штуки
    private fun match(vararg kinds: TokenKind): Boolean {
        if (kinds.any { check(it) }) {
            advance()
            return true
        }
        return false
    }

    private fun expect(kind: TokenKind, msg: String): Token {
        if (check(kind)) return advance()
        error("Ожидалось: $msg")
    }

    private fun check(kind: TokenKind) =
        !isAtEnd() && peek().kind == kind

    private fun advance(): Token = tokens[pos++]
    private fun peek(): Token = tokens[pos]
    private fun previous(): Token = tokens[pos - 1]
    private fun isAtEnd(): Boolean = peek().kind == TokenKind.EOF

    private fun isTypeStart(kind: TokenKind) =
        kind == TokenKind.KW_INT ||
                kind == TokenKind.KW_BOOL ||
                kind == TokenKind.IDENT

    private fun error(msg: String): Nothing {
        diagnostics += Diagnostic("Parser Error: $msg", peek().span)
        throw ParserPanic(msg)
    }

    private fun mapTokenTypeToOperator(kind: TokenKind): Operator {
        return when (kind) {
            TokenKind.PLUS -> Operator.PLUS
            TokenKind.MINUS -> Operator.MINUS
            TokenKind.STAR -> Operator.MULTIPLY
            TokenKind.SLASH -> Operator.DIVIDE
            TokenKind.PERCENT -> Operator.REMAINING

            TokenKind.BANG -> Operator.NOT
            TokenKind.EQEQ -> Operator.EQUAL
            TokenKind.NEQ -> Operator.NOT_EQUAL
            TokenKind.GT -> Operator.GREATER
            TokenKind.LT -> Operator.LESS
            TokenKind.GTEQ -> Operator.GREATER_EQUAL
            TokenKind.LTEQ -> Operator.LESS_EQUAL

            else -> error("Token $kind is not an operator")
        }
    }

    private class ParserPanic(msg: String) : RuntimeException(msg)
}
