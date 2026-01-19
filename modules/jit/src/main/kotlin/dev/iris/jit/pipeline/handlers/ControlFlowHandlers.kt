package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.OpCode
import dev.iris.core.bytecode.Instr
import dev.iris.jit.runtime.MissingOperandException
import dev.iris.jit.runtime.StackUnderflowException
import dev.iris.jit.runtime.UnsupportedOpcodeException

object JmpHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val target = instr.operand?.toInt() ?: throw MissingOperandException("JMP")
        ctx.ip = target - 1
    }
}

object JmpIfFalseHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val target = instr.operand?.toInt() ?: throw MissingOperandException("JMP_IF_FALSE")
        if (ctx.stack.isEmpty()) throw StackUnderflowException("JMP_IF_FALSE")
        val condition = ctx.stack.removeLast()
        if (condition == 0L) {
            ctx.ip = target - 1
        }
    }
}

object JmpIfTrueHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val target = instr.operand?.toInt() ?: throw MissingOperandException("JMP_IF_TRUE")
        if (ctx.stack.isEmpty()) throw StackUnderflowException("JMP_IF_TRUE")
        val condition = ctx.stack.removeLast()
        if (condition != 0L) {
            ctx.ip = target - 1
        }
    }
}

object CallHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        throw UnsupportedOpcodeException(OpCode.CALL)
    }
}

object RetHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        if (ctx.stack.isEmpty()) throw StackUnderflowException("RET")
        ctx.returnValue = ctx.stack.removeLast()
        ctx.halted = true
    }
}

object RetVoidHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        ctx.halted = true
    }
}
