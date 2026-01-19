package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.Instr

interface OpHandler {
    fun handle(ctx: ExecutionContext, instr: Instr)
}
