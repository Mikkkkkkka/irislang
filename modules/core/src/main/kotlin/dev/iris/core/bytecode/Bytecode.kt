package dev.iris.core.bytecode

enum class OpCode {
    PUSH_I64,
    PUSH_TRUE,
    PUSH_FALSE,
    POP,
    DUP,

    ADD,
    SUB,
    MUL,
    DIV,
    MOD,
    NEG,

    CMP_EQ,
    CMP_NE,
    CMP_LT,
    CMP_LE,
    CMP_GT,
    CMP_GE,

    AND,
    OR,
    NOT,

    LOAD_LOCAL,
    STORE_LOCAL,
    LOAD_GLOBAL,
    STORE_GLOBAL,

    JMP,
    JMP_IF_FALSE,
    JMP_IF_TRUE,
    CALL,
    RET,
    RET_VOID,

    ALLOC_ARR,
    LOAD_ARR,
    STORE_ARR,
    ALLOC_STRUCT,
    LOAD_FIELD,
    STORE_FIELD,
    NEW,

    PRINT_I64,
    PRINT_BOOL,

    HALT
}

data class Instr(
    val op: OpCode,
    val operand: Long? = null
)

data class BytecodeProgram(
    val instructions: List<Instr>
)
