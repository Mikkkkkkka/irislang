package dev.iris.core.ast

import dev.iris.core.Span

sealed interface Stmt {

    val span: Span?

    data class PrintInt(
        val value: Long,
        override val span: Span? = null
    ) : Stmt
}

