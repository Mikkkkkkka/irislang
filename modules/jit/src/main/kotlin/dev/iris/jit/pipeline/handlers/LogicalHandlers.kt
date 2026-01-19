package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.Instr
import dev.iris.jit.runtime.StackUnderflowException

object AndHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("AND")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a != 0L && b != 0L) 1L else 0L)
    }
}

object OrHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("OR")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a != 0L || b != 0L) 1L else 0L)
    }
}

object NotHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.isEmpty()) throw StackUnderflowException("NOT")
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a == 0L) 1L else 0L)
    }
}
