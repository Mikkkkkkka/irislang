package dev.iris.jit.api

data class FunctionMeta(
    val arity: Int,
    val localsCount: Int,
    val stackLimit: Int,
    val globalsCount: Int = 256,
    val startIp: Int = 0
)
