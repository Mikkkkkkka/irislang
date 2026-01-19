package dev.iris.parser.parser

import dev.iris.core.Diagnostic
import dev.iris.core.Span
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
    private var pos: Int = 0
    private val diagnostics = mutableListOf<Diagnostic>()

    fun parse(): ParseResult {
        val decls = mutableListOf<Decl>()
        val stmts = mutableListOf<Stmt>()

        while (!isAtEnd()) {
            // Skip stray semicolons to reduce cascading errors
            while (match(TokenKind.SEMICOLON)) { /* ignore */ }
            if (isAtEnd()) break

            val parsedDecl = when (peek().kind) {
                TokenKind.KW_STRUCTURE -> parseStructDeclOrRecover()
                TokenKind.KW_PROCEDURE -> parseProcedureDeclOrRecover()
                TokenKind.KW_FUNCTION -> parseFunctionDeclOrRecover()
                else -> null
            }

            if (parsedDecl != null) {
                decls += parsedDecl
            } else {
                val stmt = parseStmtOrRecover()
                if (stmt != null) stmts += stmt
            }
        }

        return ParseResult(
            Program(declarations = decls, statements = stmts),
            diagnostics
        )
    }

    // Декларации
    private fun parseStructDeclOrRecover(): Decl.Structure? =
        try {
            parseStructDecl()
        } catch (_: ParsePanic) {
            synchronizeTopLevel()
            null
        }

    private fun parseProcedureDeclOrRecover(): Decl.Procedure? =
        try {
            parseProcedureDecl()
        } catch (_: ParsePanic) {
            synchronizeTopLevel()
            null
        }

    private fun parseFunctionDeclOrRecover(): Decl.Function? =
        try {
            parseFunctionDecl()
        } catch (_: ParsePanic) {
            synchronizeTopLevel()
            null
        }

    private fun parseStructDecl(): Decl.Structure {
        expect(TokenKind.KW_STRUCTURE, "Ожидалось ключевое слово 'структура'.")
        val nameTok = expect(TokenKind.IDENT, "Ожидалось имя структуры после 'структура'.")
        expect(TokenKind.KW_START, "Ожидалось 'нч' после объявления структуры.")

        val fields = mutableListOf<Stmt.VarDecl>()
        while (!check(TokenKind.KW_END) && !isAtEnd()) {
            val f = parseFieldDeclOrRecover()
            if (f != null) fields += f
        }

        expect(TokenKind.KW_END, "Ожидалось 'кц' в конце структуры.")
        return Decl.Structure(nameTok.lexeme, fields)
    }

    private fun parseFieldDeclOrRecover(): Stmt.VarDecl? =
        try {
            val type = parseTypeRef()
            val nameTok = expect(TokenKind.IDENT, "Ожидалось имя поля структуры после типа.")
            expectStmtTerminator("Ожидался ';' после объявления поля структуры.")
            Stmt.VarDecl(name = nameTok.lexeme, type = type)
        } catch (_: ParsePanic) {
            synchronizeInBlock()
            null
        }

    private fun parseProcedureDecl(): Decl.Procedure {
        if (match(TokenKind.KW_PROCEDURE)) {
            val nameTok = expect(TokenKind.IDENT, "Ожидалось имя процедуры после 'процедура'.")
            expect(TokenKind.LPAREN, "Ожидалась '(' после имени процедуры.")
            val params = parseParams()
            expect(TokenKind.RPAREN, "Ожидалась ')' после параметров.")
            val body = parseBlock()
            return Decl.Procedure(
                name = nameTok.lexeme,
                params = params,
                body = body
            )
        } else {
            errorHere("Ожидалось 'процедура'.")
            throw ParsePanic()
        }
    }

    private fun parseFunctionDecl(): Decl.Function {
        if (match(TokenKind.KW_FUNCTION)) {
            expect(TokenKind.COLON, "Ожидался ':' после 'функция'.")
            val returnType = parseTypeRef()
            val nameTok = expect(TokenKind.IDENT, "Ожидалось имя функции.")
            expect(TokenKind.LPAREN, "Ожидалась '(' после имени функции.")
            val params = parseParams()
            expect(TokenKind.RPAREN, "Ожидалась ')' после параметров.")
            val body = parseBlock()
            return Decl.Function(
                name = nameTok.lexeme,
                returnType = returnType,
                params = params,
                body = body
            )
        } else {
            errorHere("Ожидалось 'функция'.")
            throw ParsePanic()
        }
    }

    private fun parseParams(): List<Stmt.VarDecl> {
        val params = mutableListOf<Stmt.VarDecl>()
        if (check(TokenKind.RPAREN)) return params

        do {
            val type = parseTypeRef()
            val nameTok = expect(TokenKind.IDENT, "Ожидалось имя параметра после типа.")
            params += Stmt.VarDecl(name = nameTok.lexeme, type = type)
        } while (match(TokenKind.COMMA))

        return params
    }

    // "Утверждения" (statements)
    private fun parseStmtOrRecover(): Stmt? =
        try {
            parseStmt()
        } catch (_: ParsePanic) {
            synchronizeInBlock()
            null
        }

    private fun parseStmt(): Stmt {
        return when {
            check(TokenKind.KW_START) -> parseBlock()

            match(TokenKind.KW_IF) -> parseIfStmt()

            match(TokenKind.KW_WHILE) -> parseWhileStmt()

            match(TokenKind.KW_RETURN) -> {
                val value = if (check(TokenKind.SEMICOLON)) null else parseExpr()
                expectStmtTerminator("Ожидался ';' после 'вернуть'.")
                Stmt.Return(value)
            }

            match(TokenKind.KW_BREAK) -> {
                expectStmtTerminator("Ожидался ';' после 'выйти'.")
                Stmt.Break()
            }

            match(TokenKind.KW_CONTINUE) -> {
                expectStmtTerminator("Ожидался ';' после 'продолжить'.")
                Stmt.Continue()
            }

            match(TokenKind.KW_PRINT) -> parsePrintStmt()

            // Variable declaration begins with a type token (целч/бул) or IDENT (struct type)
            isTypeStart(peek().kind) -> parseVarDeclStmt()

            else -> {
                val expr = parseExpr()
                expectStmtTerminator("Ожидался ';' после выражения.")
                Stmt.ExprStmt(expr)
            }
        }
    }

    private fun parseBlock(): Stmt.Block {
        expect(TokenKind.KW_START, "Ожидалось 'нч' для начала блока.")
        val stmts = mutableListOf<Stmt>()

        while (!check(TokenKind.KW_END) && !isAtEnd()) {
            // tolerate stray semicolons
            while (match(TokenKind.SEMICOLON)) { /* ignore */
            }
            if (check(TokenKind.KW_END) || isAtEnd()) break

            val stmt = parseStmtOrRecover()
            if (stmt != null) stmts += stmt
        }

        expect(TokenKind.KW_END, "Ожидалось 'кц' в конце блока.")
        return Stmt.Block(stmts)
    }

    private fun parseIfStmt(): Stmt.If {
        expect(TokenKind.LPAREN, "Ожидалась '(' после 'если'.")
        val cond = parseExpr()
        expect(TokenKind.RPAREN, "Ожидалась ')' после условия 'если'.")
        val thenBlock = parseBlock()

        var elseBranch: Stmt? = null
        if (match(TokenKind.KW_ELSE)) {
            elseBranch = if (match(TokenKind.KW_IF)) {
                expect(TokenKind.LPAREN, "Ожидалась '(' после 'иначе если'.")
                val c = parseExpr()
                expect(TokenKind.RPAREN, "Ожидалась ')' после условия 'иначе если'.")
                val b = parseBlock()
                Stmt.If(c, b, parseIfTailMaybe())
            } else {
                parseBlock()
            }
        }

        return Stmt.If(cond, thenBlock, elseBranch)
    }

    private fun parseIfTailMaybe(): Stmt? {
        if (!match(TokenKind.KW_ELSE)) return null
        if (match(TokenKind.KW_IF)) {
            expect(TokenKind.LPAREN, "Ожидалась '(' после 'иначе если'.")
            val c = parseExpr()
            expect(TokenKind.RPAREN, "Ожидалась ')' после условия 'иначе если'.")
            val b = parseBlock()
            return Stmt.If(c, b, parseIfTailMaybe())
        }
        return parseBlock()
    }

    private fun parseWhileStmt(): Stmt.While {
        expect(TokenKind.LPAREN, "Ожидалась '(' после 'пока'.")
        val cond = parseExpr()
        expect(TokenKind.RPAREN, "Ожидалась ')' после условия 'пока'.")
        val body = parseBlock()
        return Stmt.While(cond, body)
    }

    private fun parseVarDeclStmt(): Stmt.VarDecl {
        val type = parseTypeRef()
        val nameTok = expect(TokenKind.IDENT, "Ожидалось имя переменной после типа.")

        val init = if (match(TokenKind.EQ)) {
            parseExpr()
        } else null

        expectStmtTerminator("Ожидался ';' после объявления переменной.")
        return Stmt.VarDecl(type = type, name = nameTok.lexeme, init = init)
    }

    private fun parsePrintStmt(): Stmt {
        expect(TokenKind.COLON, "Ожидался ':' после 'печать'.")

        val arg: Expr = if (match(TokenKind.LPAREN)) {
            val e = parseExpr()
            expect(TokenKind.RPAREN, "Ожидалась ')' после 'печать:(...)'.")
            e
        } else {
            // single token argument: number/bool/null/identifier (spec mentions these)
            // We'll accept general primary-expr start here to be slightly more tolerant.
            parsePrimary()
        }

        expectStmtTerminator("Ожидался ';' после 'печать:...'.")
        // Choose representation:
        // 1) if you have Stmt.Print -> return Stmt.Print(arg)
        // 2) otherwise encode as call
        return Stmt.ExprStmt(Expr.Call(callee = "печать", args = listOf(arg)))
    }

    // Типы
    private fun parseTypeRef(): TypeRef {
        val base: TypeRef = when {
            match(TokenKind.KW_INT) -> TypeRef.IntType
            match(TokenKind.KW_BOOL) -> TypeRef.BoolType
            check(TokenKind.IDENT) -> {
                val nameTok = advance()
                TypeRef.StructType(nameTok.lexeme)
            }

            else -> {
                errorHere("Ожидался тип: 'целч', 'бул' или имя структуры.")
                throw ParsePanic()
            }
        }

        var t = base
        while (match(TokenKind.STAR)) {
            t = TypeRef.Pointer(t)
        }

        return t
    }

    private fun isTypeStart(kind: TokenKind): Boolean =
        kind == TokenKind.KW_INT || kind == TokenKind.KW_BOOL || kind == TokenKind.IDENT && tokens[pos + 1].kind == TokenKind.IDENT   // Очень криво проверяем, "а точно ли это кастомная структура?"

    // Выражения
    /**
     * Приоритет (low -> high):
     *  - присваивание: =
     *  - или: `или`
     *  - и: `и`
     *  - равность: == !=
     *  - сравнение: < <= > >=
     *  - сложение: + -
     *  - умножение: * / %
     *  - префиксы: !  ?  *  -   (note: `*` is both prefix deref and infix multiply)
     *  - постфиксы: . field, ( ) call
     */
    private fun parseExpr(): Expr = parseAssignment()

    private fun parseAssignment(): Expr {
        val left = parseOr()
        if (match(TokenKind.EQ)) {
            val value = parseAssignment()
            return Expr.Assign(left, value)
        }
        return left
    }

    private fun parseOr(): Expr {
        var expr = parseAnd()
        while (match(TokenKind.KW_OR)) {
            val right = parseAnd()
            expr = Expr.Binary(expr, Operator.OR, right)
        }
        return expr
    }

    private fun parseAnd(): Expr {
        var expr = parseEquality()
        while (match(TokenKind.KW_AND)) {
            val right = parseEquality()
            expr = Expr.Binary(expr, Operator.AND, right)
        }
        return expr
    }

    private fun parseEquality(): Expr {
        var expr = parseComparison()
        while (true) {
            expr = when {
                match(TokenKind.EQEQ) -> Expr.Binary(expr, Operator.EQUAL, parseComparison())
                match(TokenKind.NEQ) -> Expr.Binary(expr, Operator.NOT_EQUAL, parseComparison())
                else -> return expr
            }
        }
    }

    private fun parseComparison(): Expr {
        var expr = parseAdditive()
        while (true) {
            expr = when {
                match(TokenKind.LT) -> Expr.Binary(expr, Operator.LESS, parseAdditive())
                match(TokenKind.LTEQ) -> Expr.Binary(expr, Operator.LESS_EQUAL, parseAdditive())
                match(TokenKind.GT) -> Expr.Binary(expr, Operator.GREATER, parseAdditive())
                match(TokenKind.GTEQ) -> Expr.Binary(expr, Operator.GREATER_EQUAL, parseAdditive())
                else -> return expr
            }
        }
    }

    private fun parseAdditive(): Expr {
        var expr = parseMultiplicative()
        while (true) {
            expr = when {
                match(TokenKind.PLUS) -> Expr.Binary(expr, Operator.ADD, parseMultiplicative())
                match(TokenKind.MINUS) -> Expr.Binary(expr, Operator.SUBTRACT, parseMultiplicative())
                else -> return expr
            }
        }
    }

    private fun parseMultiplicative(): Expr {
        var expr = parsePrefix()
        while (true) {
            expr = when {
                match(TokenKind.STAR) -> Expr.Binary(expr, Operator.MULTIPLY, parsePrefix())
                match(TokenKind.SLASH) -> Expr.Binary(expr, Operator.DIVIDE, parsePrefix())
                match(TokenKind.PERCENT) -> Expr.Binary(expr, Operator.REMAINING, parsePrefix())
                else -> return expr
            }
        }
    }

    private fun parsePrefix(): Expr {
        return when {
            match(TokenKind.BANG) -> Expr.Unary(Operator.NOT, parsePrefix())
            match(TokenKind.QUESTION) -> Expr.Unary(Operator.ADDRESS, parsePrefix())
            match(TokenKind.STAR) -> Expr.Unary(Operator.DEREFERENCE, parsePrefix())
            match(TokenKind.MINUS) -> Expr.Unary(Operator.NEGATIVE, parsePrefix())
            else -> parsePostfix(parsePrimary())
        }
    }

    private fun parsePostfix(start: Expr): Expr {
        var expr = start
        while (true) {
            expr = when {
                match(TokenKind.DOT) -> {
                    val fieldTok = expect(TokenKind.IDENT, "Ожидалось имя поля после '.'.")
                    Expr.FieldAccess(target = expr, field = fieldTok.lexeme)
                }

                match(TokenKind.LPAREN) -> {
                    val args = parseCallArgsAlreadyOpened()
                    val calleeName = when (expr) {
                        is Expr.Var -> expr.name
                        else -> {
                            errorAdd("Вызов функции ожидает имя функции (идентификатор).")
                            "<invalid>"
                        }
                    }
                    Expr.Call(callee = calleeName, args = args)
                }

                else -> return expr
            }
        }
    }

    private fun parseCallArgsAlreadyOpened(): List<Expr> {
        // '(' already consumed
        val args = mutableListOf<Expr>()
        if (check(TokenKind.RPAREN)) {
            advance()
            return args
        }

        do {
            args += parseExpr()
        } while (match(TokenKind.COMMA))

        expect(TokenKind.RPAREN, "Ожидалась ')' после аргументов вызова.")
        return args
    }

    private fun parsePrimary(): Expr {
        if (match(TokenKind.KW_NEW)) {
            expect(TokenKind.COLON, "Ожидался ':' после 'новый'.")
            val t = parseTypeRef()

            var size: Expr? = null
            if (match(TokenKind.COLON)) {
                size = if (match(TokenKind.LPAREN)) {
                    val e = parseExpr()
                    expect(TokenKind.RPAREN, "Ожидалась ')' после выражения размера в 'новый:тип:(...)'.")
                    e
                } else {
                    when {
                        match(TokenKind.INT_LITERAL) -> {
                            val tok = previous()
                            Expr.IntLiteral(tok.lexeme.toIntOrNull() ?: 0)
                        }

                        match(TokenKind.IDENT) -> {
                            val tok = previous()
                            Expr.Var(tok.lexeme)
                        }

                        else -> {
                            errorHere("Ожидался размер: число, идентификатор или '(выражение)'.")
                            throw ParsePanic()
                        }
                    }
                }
            }

            return Expr.New(type = t, size = size)
        }

        if (match(TokenKind.INT_LITERAL)) {
            val tok = previous()
            return Expr.IntLiteral(tok.lexeme.toIntOrNull() ?: 0)
        }

        if (match(TokenKind.KW_TRUE)) return Expr.BoolLiteral(true)
        if (match(TokenKind.KW_FALSE)) return Expr.BoolLiteral(false)
        if (match(TokenKind.KW_NULL)) return Expr.NullLiteral

        if (match(TokenKind.IDENT)) {
            val tok = previous()
            return Expr.Var(tok.lexeme)
        }

        if (match(TokenKind.LPAREN)) {
            val e = parseExpr()
            expect(TokenKind.RPAREN, "Ожидалась ')' после выражения в скобках.")
            return e
        }

        errorHere("Ожидалось выражение.")
        throw ParsePanic()
    }

    // Завершение выражений
    private fun expectStmtTerminator(message: String) {
        if (match(TokenKind.SEMICOLON)) return

        errorAdd(message, peek().span)

        if (check(TokenKind.KW_END) || check(TokenKind.EOF)) return

        while (!isAtEnd() && !check(TokenKind.SEMICOLON) && !check(TokenKind.KW_END)) {
            if (isStmtStart(peek().kind)) return
            advance()
        }
        match(TokenKind.SEMICOLON)
    }

    private fun isStmtStart(kind: TokenKind): Boolean =
        kind == TokenKind.KW_IF ||
                kind == TokenKind.KW_WHILE ||
                kind == TokenKind.KW_RETURN ||
                kind == TokenKind.KW_BREAK ||
                kind == TokenKind.KW_CONTINUE ||
                kind == TokenKind.KW_PRINT ||
                kind == TokenKind.KW_START ||
                isTypeStart(kind)

    // Восстановление
    private fun synchronizeTopLevel() {
        while (!isAtEnd()) {
            if (check(TokenKind.KW_STRUCTURE) ||
                check(TokenKind.KW_PROCEDURE) ||
                check(TokenKind.KW_FUNCTION) ||
                check(TokenKind.KW_IF) ||
                check(TokenKind.KW_WHILE) ||
                check(TokenKind.KW_PRINT) ||
                isTypeStart(peek().kind)
            ) return

            advance()
        }
    }

    private fun synchronizeInBlock() {
        while (!isAtEnd()) {
            if (previousOrNull()?.kind == TokenKind.SEMICOLON) return
            if (check(TokenKind.KW_END)) return
            if (isStmtStart(peek().kind)) return
            advance()
        }
    }

    // --------------

    private fun expect(kind: TokenKind, message: String): Token {
        if (check(kind)) return advance()
        errorAdd(message, peek().span)
        throw ParsePanic()
    }

    private fun match(kind: TokenKind): Boolean {
        if (check(kind)) {
            advance()
            return true
        }
        return false
    }

    private fun check(kind: TokenKind): Boolean =
        !isAtEnd() && peek().kind == kind

    private fun advance(): Token {
        if (!isAtEnd()) pos++
        return previous()
    }

    private fun peek(): Token = tokens[pos]
    private fun previous(): Token = tokens[pos - 1]
    private fun previousOrNull(): Token? = if (pos > 0) tokens[pos - 1] else null
    private fun isAtEnd(): Boolean = peek().kind == TokenKind.EOF

    private fun errorHere(message: String) {
        errorAdd(message, peek().span, Diagnostic.Severity.ERROR)
    }

    private fun errorAdd(message: String, span: Span? = null, severity: Diagnostic.Severity = Diagnostic.Severity.WARNING) {
        diagnostics += Diagnostic(message, span, severity)
    }

    private class ParsePanic : RuntimeException()
}
