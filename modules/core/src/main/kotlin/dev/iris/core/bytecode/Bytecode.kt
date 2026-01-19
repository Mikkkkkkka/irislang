package dev.iris.core.bytecode

/**
 * Complete instruction set for IRIS VM.
 */
enum class OpCode {
    // Constants
    PUSH_I64,       // Push i64 constant onto stack
    PUSH_TRUE,      // Push boolean true
    PUSH_FALSE,     // Push boolean false

    // Arithmetic
    ADD,            // Pop b, pop a, push a+b
    SUB,            // Pop b, pop a, push a-b
    MUL,            // Pop b, pop a, push a*b
    DIV,            // Pop b, pop a, push a/b
    MOD,            // Pop b, pop a, push a%b
    NEG,            // Pop a, push -a

    // Comparison
    CMP_EQ,         // Pop b, pop a, push a==b (1 or 0)
    CMP_NE,         // Pop b, pop a, push a!=b
    CMP_LT,         // Pop b, pop a, push a<b
    CMP_LE,         // Pop b, pop a, push a<=b
    CMP_GT,         // Pop b, pop a, push a>b
    CMP_GE,         // Pop b, pop a, push a>=b

    // Logical
    AND,            // Pop b, pop a, push a&&b
    OR,             // Pop b, pop a, push a||b
    NOT,            // Pop a, push !a

    // Control flow
    JMP,            // Unconditional jump to operand address
    JMP_IF_FALSE,   // Pop condition, jump if false
    JMP_IF_TRUE,    // Pop condition, jump if true

    // Variables
    LOAD_LOCAL,     // Push local variable (operand = index)
    STORE_LOCAL,    // Pop value, store to local (operand = index)
    LOAD_GLOBAL,    // Push global variable (operand = index)
    STORE_GLOBAL,   // Pop value, store to global (operand = index)

    // Functions
    CALL,           // Call function (operand = function index)
    RET,            // Return from function (pop return value if any)
    RET_VOID,       // Return from procedure (no value)

    // Arrays
    ALLOC_ARR,      // Pop size, allocate array, push ref
    LOAD_ARR,       // Pop index, pop array_ref, push element
    STORE_ARR,      // Pop value, pop index, pop array_ref, store

    // Structs
    ALLOC_STRUCT,   // Allocate struct (operand = struct type index)
    LOAD_FIELD,     // Pop struct_ref, push field (operand = field index)
    STORE_FIELD,    // Pop value, pop struct_ref, store field (operand = field index)

    // Pointers (heap allocation)
    NEW,            // Allocate on heap (operand = type info)

    // I/O
    PRINT_I64,      // Pop i64, print it
    PRINT_BOOL,     // Pop bool, print it

    // Stack manipulation
    POP,            // Discard top of stack
    DUP,            // Duplicate top of stack

    // Program control
    HALT            // Stop execution
}

data class Instr(
    val op: OpCode,
    val operand: Long? = null
)

data class BytecodeProgram(
    val instructions: List<Instr>,
    val constPool: List<Long> = emptyList(),
    val functions: List<FunctionInfo> = emptyList()
)

/**
 * Metadata about a function in the bytecode.
 */
data class FunctionInfo(
    val name: String,
    val startIp: Int,           // First instruction of function
    val paramCount: Int,        // Number of parameters
    val localCount: Int,        // Total local variables (including params)
    val returnsValue: Boolean   // true for function, false for procedure
)
