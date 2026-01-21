package dev.iris.jit.pipeline

import dev.iris.core.bytecode.Instr
import dev.iris.jit.api.FunctionMeta

/**
 * Intermediate lowered form for one function; currently mirrors bytecode 1:1.
 */
data class LoweredFunction(
    val funcIndex: Int,
    val startIp: Int,
    val meta: FunctionMeta,
    val instructions: List<Instr>
)
