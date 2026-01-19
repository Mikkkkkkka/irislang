package dev.iris.jit.runtime

import dev.iris.core.bytecode.OpCode

sealed class JitRuntimeException(message: String) : RuntimeException(message)

class StackUnderflowException(op: String) :
    JitRuntimeException("Stack underflow at $op")

class MissingOperandException(op: String) :
    JitRuntimeException("Missing operand for $op")

class UnsupportedOpcodeException(op: OpCode) :
    JitRuntimeException("Unsupported opcode: $op")

class DivisionByZeroException :
    JitRuntimeException("Division by zero")

class ArrayOutOfBoundsException(index: Int, size: Int) :
    JitRuntimeException("Array index $index out of bounds for size $size")

class NullPointerException(op: String) :
    JitRuntimeException("Null pointer dereference at $op")

class InvalidHeapReferenceException(ptr: Long) :
    JitRuntimeException("Invalid heap reference: $ptr")

class InvalidVariableIndexException(kind: String, index: Int, size: Int) :
    JitRuntimeException("$kind variable index $index out of bounds for size $size")
