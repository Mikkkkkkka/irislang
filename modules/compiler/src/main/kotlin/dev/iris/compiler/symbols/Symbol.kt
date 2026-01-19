package dev.iris.compiler.symbols

import dev.iris.core.ast.TypeRef

sealed class Symbol {
    abstract val name: String
    abstract val type: TypeRef

    data class Variable(
        override val name: String,
        override val type: TypeRef,
        val index: Int,
        val isGlobal: Boolean
    ) : Symbol()

    data class Function(
        override val name: String,
        override val type: TypeRef,
        val index: Int,
        val paramCount: Int,
        val returnsValue: Boolean
    ) : Symbol()

    data class Structure(
        override val name: String,
        override val type: TypeRef,
        val index: Int,
        val fields: List<Pair<String, TypeRef>>
    ) : Symbol()
}
