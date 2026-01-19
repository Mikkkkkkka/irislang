package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.Instr
import dev.iris.jit.runtime.MissingOperandException
import dev.iris.jit.runtime.StackUnderflowException

object PushI64Handler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val value = instr.operand ?: throw MissingOperandException("PUSH_I64")
        ctx.stack.addLast(value)
    }
}

object PushTrueHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        ctx.stack.addLast(1L)
    }
}

object PushFalseHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        ctx.stack.addLast(0L)
    }
}

object PopHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.isEmpty()) throw StackUnderflowException("POP")
        ctx.stack.removeLast()
    }
}

object DupHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val top = ctx.stack.lastOrNull() ?: throw StackUnderflowException("DUP")
        ctx.stack.addLast(top)
    }
}
