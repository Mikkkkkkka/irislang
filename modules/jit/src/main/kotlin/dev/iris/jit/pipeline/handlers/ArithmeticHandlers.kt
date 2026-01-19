package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.Instr
import dev.iris.jit.runtime.DivisionByZeroException
import dev.iris.jit.runtime.StackUnderflowException

object AddHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("ADD")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(a + b)
    }
}

object SubHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("SUB")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(a - b)
    }
}

object MulHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("MUL")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(a * b)
    }
}

object DivHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("DIV")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        if (b == 0L) throw DivisionByZeroException()
        ctx.stack.addLast(a / b)
    }
}

object ModHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.size < 2) throw StackUnderflowException("MOD")
        val b = ctx.stack.removeLast()
        val a = ctx.stack.removeLast()
        if (b == 0L) throw DivisionByZeroException()
        ctx.stack.addLast(a % b)
    }
}

object NegHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.isEmpty()) throw StackUnderflowException("NEG")
        val a = ctx.stack.removeLast()
        ctx.stack.addLast(-a)
    }
}
