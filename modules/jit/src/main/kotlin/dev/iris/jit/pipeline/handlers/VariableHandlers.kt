package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.Instr
import dev.iris.jit.runtime.InvalidVariableIndexException
import dev.iris.jit.runtime.MissingOperandException
import dev.iris.jit.runtime.StackUnderflowException

object LoadLocalHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val index = instr.operand?.toInt() ?: throw MissingOperandException("LOAD_LOCAL")
        if (index < 0 || index >= ctx.locals.size) {
            throw InvalidVariableIndexException("Local", index, ctx.locals.size)
        }
        ctx.stack.addLast(ctx.locals[index])
    }
}

object StoreLocalHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val index = instr.operand?.toInt() ?: throw MissingOperandException("STORE_LOCAL")
        if (index < 0 || index >= ctx.locals.size) {
            throw InvalidVariableIndexException("Local", index, ctx.locals.size)
        }
        if (ctx.stack.isEmpty()) throw StackUnderflowException("STORE_LOCAL")
        val value = ctx.stack.removeLast()
        ctx.locals[index] = value
    }
}

object LoadGlobalHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val index = instr.operand?.toInt() ?: throw MissingOperandException("LOAD_GLOBAL")
        if (index < 0 || index >= ctx.globals.size) {
            throw InvalidVariableIndexException("Global", index, ctx.globals.size)
        }
        ctx.stack.addLast(ctx.globals[index])
    }
}

object StoreGlobalHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        val index = instr.operand?.toInt() ?: throw MissingOperandException("STORE_GLOBAL")
        if (index < 0 || index >= ctx.globals.size) {
            throw InvalidVariableIndexException("Global", index, ctx.globals.size)
        }
        if (ctx.stack.isEmpty()) throw StackUnderflowException("STORE_GLOBAL")
        val value = ctx.stack.removeLast()
        ctx.globals[index] = value
    }
}
