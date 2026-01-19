package dev.iris.jit.pipeline.handlers

import dev.iris.core.bytecode.OpCode
import dev.iris.jit.runtime.UnsupportedOpcodeException
import java.util.EnumMap

class HandlerRegistry {
    private val handlers = EnumMap<OpCode, OpHandler>(OpCode::class.java)

    init {
        registerAll()
    }

    fun get(op: OpCode): OpHandler =
        handlers[op] ?: throw UnsupportedOpcodeException(op)

    private fun registerAll() {
        handlers[OpCode.PUSH_I64] = PushI64Handler
        handlers[OpCode.PUSH_TRUE] = PushTrueHandler
        handlers[OpCode.PUSH_FALSE] = PushFalseHandler
        handlers[OpCode.POP] = PopHandler
        handlers[OpCode.DUP] = DupHandler

        handlers[OpCode.ADD] = AddHandler
        handlers[OpCode.SUB] = SubHandler
        handlers[OpCode.MUL] = MulHandler
        handlers[OpCode.DIV] = DivHandler
        handlers[OpCode.MOD] = ModHandler
        handlers[OpCode.NEG] = NegHandler

        handlers[OpCode.CMP_EQ] = CmpEqHandler
        handlers[OpCode.CMP_NE] = CmpNeHandler
        handlers[OpCode.CMP_LT] = CmpLtHandler
        handlers[OpCode.CMP_LE] = CmpLeHandler
        handlers[OpCode.CMP_GT] = CmpGtHandler
        handlers[OpCode.CMP_GE] = CmpGeHandler

        handlers[OpCode.AND] = AndHandler
        handlers[OpCode.OR] = OrHandler
        handlers[OpCode.NOT] = NotHandler

        handlers[OpCode.LOAD_LOCAL] = LoadLocalHandler
        handlers[OpCode.STORE_LOCAL] = StoreLocalHandler
        handlers[OpCode.LOAD_GLOBAL] = LoadGlobalHandler
        handlers[OpCode.STORE_GLOBAL] = StoreGlobalHandler

        handlers[OpCode.JMP] = JmpHandler
        handlers[OpCode.JMP_IF_FALSE] = JmpIfFalseHandler
        handlers[OpCode.JMP_IF_TRUE] = JmpIfTrueHandler
        handlers[OpCode.CALL] = CallHandler
        handlers[OpCode.RET] = RetHandler
        handlers[OpCode.RET_VOID] = RetVoidHandler

        handlers[OpCode.ALLOC_ARR] = AllocArrHandler
        handlers[OpCode.LOAD_ARR] = LoadArrHandler
        handlers[OpCode.STORE_ARR] = StoreArrHandler
        handlers[OpCode.ALLOC_STRUCT] = AllocStructHandler
        handlers[OpCode.LOAD_FIELD] = LoadFieldHandler
        handlers[OpCode.STORE_FIELD] = StoreFieldHandler
        handlers[OpCode.NEW] = NewHandler

        handlers[OpCode.PRINT_I64] = PrintI64Handler
        handlers[OpCode.PRINT_BOOL] = PrintBoolHandler
    }
}
