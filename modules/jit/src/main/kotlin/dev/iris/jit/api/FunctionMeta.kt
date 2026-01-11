package dev.iris.jit.api

/**
 * Immutable metadata about a single function.
 */
data class FunctionMeta(
    val arity: Int,
    val localsCount: Int,
    val stackLimit: Int
)
