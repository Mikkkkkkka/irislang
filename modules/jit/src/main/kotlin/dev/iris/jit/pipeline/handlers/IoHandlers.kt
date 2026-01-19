package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.Instr
import dev.iris.jit.runtime.StackUnderflowException

object PrintI64Handler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.isEmpty()) throw StackUnderflowException("PRINT_I64")
        val value = ctx.stack.removeLast()
        ctx.printSink(value.toString())
    }
}

object PrintBoolHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.isEmpty()) throw StackUnderflowException("PRINT_BOOL")
        val value = ctx.stack.removeLast()
        ctx.printSink(if (value != 0L) "правда" else "ложь")
    }
}
