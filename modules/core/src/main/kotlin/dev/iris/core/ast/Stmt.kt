package dev.iris.core.ast

import dev.iris.core.Span

sealed interface Stmt {
    val span: Span?     // Для сообщений об ошибках

    data class Block(
        val stmts: List<Stmt>,
        override val span: Span? = null
    ) : Stmt

    data class VarDecl(
        val type: TypeRef,
        val name: String,
        override val span: Span? = null
    ) : Stmt

    data class If(
        val cond: Expr,
        val thenBranch: Block,
        val elseBranch: Stmt?,
        override val span: Span? = null
    ) : Stmt

    data class While(
        val cond: Expr,
        val body: Block,
        override val span: Span? = null,
    ) : Stmt

    data class Return(
        val expr: Expr,
        override val span: Span? = null
    ) : Stmt

    data class Break(
        override val span: Span? = null
    ) : Stmt

    data class Continue(
        override val span: Span? = null
    ) : Stmt
}