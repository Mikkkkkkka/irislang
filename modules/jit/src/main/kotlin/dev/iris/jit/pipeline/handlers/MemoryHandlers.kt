package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode
import dev.iris.jit.runtime.UnsupportedOpcodeException

object AllocArrHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        throw UnsupportedOpcodeException(OpCode.ALLOC_ARR)
    }
}

object LoadArrHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        throw UnsupportedOpcodeException(OpCode.LOAD_ARR)
    }
}

object StoreArrHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        throw UnsupportedOpcodeException(OpCode.STORE_ARR)
    }
}

object AllocStructHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        throw UnsupportedOpcodeException(OpCode.ALLOC_STRUCT)
    }
}

object LoadFieldHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        throw UnsupportedOpcodeException(OpCode.LOAD_FIELD)
    }
}

object StoreFieldHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        throw UnsupportedOpcodeException(OpCode.STORE_FIELD)
    }
}

object NewHandler : OpHandler {
    override fun handle(ctx: ExecutionContext, instr: Instr) {
        throw UnsupportedOpcodeException(OpCode.NEW)
    }
}
