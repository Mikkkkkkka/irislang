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
                TokenKind.KW_STRUCTURE -> declarations += parseStruct()
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
            fields += Stmt.VarDecl(fieldType, fieldName, null, fieldStart)
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
                params += Stmt.VarDecl(type, name, null, start)
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

        return t
    }

    // Выражения
    private fun parseExpr(): Expr = parseAssignment()

    private fun parseAssignment(): Expr {
        val left = parseBinaryExpr(0)
        if (match(TokenKind.EQ)) {
            val value = parseAssignment()
            return Expr.Assign(left, value)
        }
        return left
    }

    private fun parseBinaryExpr(minPrec: Int): Expr {
        var left = parsePrefix()

        while (true) {
            val (prec, op) = infixInfo(peek().kind) ?: break
            if (prec < minPrec) break

            advance()
            val right = parseBinaryExpr(prec + 1)
            left = Expr.Binary(left, op, right)
        }

        return left
    }

    private fun parsePrefix(): Expr {
        when {
            match(TokenKind.MINUS) -> {
                val expr = parseBinaryExpr(100)
                return Expr.Unary(Operator.MINUS, expr)
            }

            match(TokenKind.BANG) -> {
                val expr = parseBinaryExpr(100)
                return Expr.Unary(Operator.NOT, expr)
            }

            match(TokenKind.STAR) -> {
                val expr = parseBinaryExpr(100)
                return Expr.Unary(Operator.DEREFERENCE, expr)
            }

            match(TokenKind.QUESTION) -> {
                val expr = parseBinaryExpr(100)
                return Expr.Unary(Operator.ADDRESS, expr)
            }

            else -> return parsePostfix(parsePrimary())
        }
    }

    private fun parsePostfix(start: Expr): Expr {
        var expr = start

        while (true) {
            expr = when {
                match(TokenKind.DOT) -> {
                    val fieldTok = expect(TokenKind.IDENT, "Имя поля после \".\"")
                    Expr.FieldAccess(expr, fieldTok.lexeme)
                }

                match(TokenKind.LPAREN) -> {
                    val args = parseCallArgs()
                    val calleeName = when (expr) {
                        is Expr.Var -> expr.name
                        else -> error("Вызов функции ожидает имя функции")
                    }
                    Expr.Call(calleeName, args)
                }

                else -> return expr
            }
        }
    }

    private fun parseCallArgs(): List<Expr> {
        val args = mutableListOf<Expr>()
        if (check(TokenKind.RPAREN)) {
            advance()
            return args
        }

        do {
            args += parseExpr()
        } while (match(TokenKind.COMMA))

        expect(TokenKind.RPAREN, "\")\" после вызова функции")
        return args
    }

    private fun parsePrimary(): Expr {
        if (match(TokenKind.KW_PRINT)) {
            expect(TokenKind.COLON, "\":\" после \"печать\"")
            expect(TokenKind.LPAREN, "\"(\" после \"печать:\"") // TODO: add "печать:<переменная>"
            val e = parseExpr()
            expect(TokenKind.RPAREN, "\")\" после \"печать(<выражение>\"")
            return Expr.Call("печать:", listOf(e))
        }
        if (match(TokenKind.KW_NEW)) {
            expect(TokenKind.COLON, "\":\" после \"новый\"")
            val t = parseType()
            var size: Expr? = null
            if (match(TokenKind.COLON)) {
                size = parseExpr()                                          // TODO: potential problems!
            }
            return Expr.New(t, size)
        }

        if (match(TokenKind.INT_LITERAL)) {
            val tok = previous()
            return Expr.IntLiteral(tok.lexeme.toInt())              // TODO: potential problems!
        }

        if (match(TokenKind.KW_TRUE)) return Expr.BoolLiteral(true)
        if (match(TokenKind.KW_FALSE)) return Expr.BoolLiteral(false)

        if (match(TokenKind.IDENT)) {
            val tok = previous()
            return Expr.Var(tok.lexeme)
        }

        if (match(TokenKind.LPAREN)) {
            val beginParenSpan = previous().span
            val e = parseExpr()
            expect(TokenKind.RPAREN, "\")\" после \"(\" на $beginParenSpan")
            return e
        }

        error("Ожидалось выражение")
    }

    private fun infixInfo(kind: TokenKind): Pair<Int, Operator>? = when (kind) {
        TokenKind.STAR -> 50 to Operator.MULTIPLY
        TokenKind.SLASH -> 50 to Operator.DIVIDE
        TokenKind.PERCENT -> 50 to Operator.REMAINING

        TokenKind.PLUS -> 40 to Operator.PLUS
        TokenKind.MINUS -> 40 to Operator.MINUS

        TokenKind.LT -> 30 to Operator.LESS
        TokenKind.GT -> 30 to Operator.GREATER
        TokenKind.LTEQ -> 30 to Operator.LESS_EQUAL
        TokenKind.GTEQ -> 30 to Operator.GREATER_EQUAL

        TokenKind.EQEQ -> 20 to Operator.EQUAL
        TokenKind.NEQ -> 20 to Operator.NOT_EQUAL
        else -> null
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
        kind == TokenKind.KW_INT || kind == TokenKind.KW_BOOL

    private fun error(msg: String): Nothing {
        diagnostics += Diagnostic("Parser Error: $msg", peek().span)
        throw ParserPanic(msg)
    }

    private class ParserPanic(msg: String) : RuntimeException(msg)
}
