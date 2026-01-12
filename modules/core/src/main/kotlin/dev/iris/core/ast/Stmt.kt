package dev.iris.core.ast

import dev.iris.core.Span

sealed interface Stmt {
    val span: Span?

    data class VarDecl(
        val type: TypeRef,
        val name: String,
        val span: Span? = null
    )
}