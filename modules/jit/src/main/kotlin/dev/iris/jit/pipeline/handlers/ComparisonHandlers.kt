package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.Instr
import dev.iris.jit.runtime.StackUnderflowException

object CmpEqHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("CMP_EQ")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a == b) 1L else 0L)
    }
}

object CmpNeHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("CMP_NE")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a != b) 1L else 0L)
    }
}

object CmpLtHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("CMP_LT")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a < b) 1L else 0L)
    }
}

object CmpLeHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("CMP_LE")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a <= b) 1L else 0L)
    }
}

object CmpGtHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("CMP_GT")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a > b) 1L else 0L)
    }
}

object CmpGeHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("CMP_GE")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(if (a >= b) 1L else 0L)
    }
}
