package dev.iris.core.bytecode

/**
 * Minimal bytecode to make the scaffold runnable immediately.
 * Extend freely.
 */
enum class OpCode {
    PUSH_I64,
    PRINT_I64,
    HALT
}

data class Instr(
    val op: OpCode,
    val operand: Long? = null
)

data class BytecodeProgram(
    val instructions: List<Instr>
)
