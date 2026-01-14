package dev.iris.vm

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.OpCode

/**
 * Stack-based virtual machine with async JIT support.
 *
 * The VM is single-threaded for execution. JIT compilation happens
 * asynchronously in background threads.
 */
class VirtualMachine {

    // Operand stack for expression evaluation
    private val stack = ArrayDeque<Value>()

    // Call stack for function frames
    private val callStack = ArrayDeque<CallFrame>()

    // Heap for arrays and structs
    private val heap = Heap()

    // Garbage collector
    private val gc = GarbageCollector(heap)

    // Global variables
    private val globals = mutableListOf<Value>()

    // Current instruction pointer
    private var ip = 0

    /**
     * Push a value onto the operand stack.
     */
    fun push(value: Value) {
        stack.addLast(value)
    }

    /**
     * Pop a value from the operand stack.
     */
    fun pop(): Value {
        if (stack.isEmpty()) error("Stack underflow")
        return stack.removeLast()
    }

    /**
     * Peek at top of stack without removing.
     */
    fun peek(): Value {
        if (stack.isEmpty()) error("Stack underflow")
        return stack.last()
    }

    /**
     * Allocate an array on the heap.
     */
    fun allocArray(size: Int): Value.HeapRef {
        val ref = heap.allocArray(size)
        return Value.HeapRef(ref)
    }

    /**
     * Allocate a struct on the heap.
     */
    fun allocStruct(typeIndex: Int, fieldCount: Int): Value.HeapRef {
        val ref = heap.allocStruct(typeIndex, fieldCount)
        return Value.HeapRef(ref)
    }

    /**
     * Get heap object by reference.
     */
    fun getHeapObject(address: Long): HeapObject {
        return heap.get(address)
    }

    /**
     * Run garbage collection.
     * Collects from all roots: stack, call frames, globals.
     */
    fun collectGarbage() {
        val roots = mutableListOf<Value>()

        // Add operand stack
        roots.addAll(stack)

        // Add locals from all call frames
        callStack.forEach { frame ->
            roots.addAll(frame.locals)
        }

        // Add globals
        roots.addAll(globals)

        gc.collect(roots)
    }

    /**
     * Execute a bytecode program.
     */
    fun run(program: BytecodeProgram, stdout: (String) -> Unit = ::println): VmResult {
        val code = program.instructions
        ip = 0

        while (ip in code.indices) {
            val instr = code[ip]

            when (instr.op) {
                OpCode.PUSH_I64 -> {
                    val value = instr.operand ?: error("Missing operand for PUSH_I64")
                    push(Value.Int(value))
                }

                OpCode.PRINT_I64 -> {
                    val value = pop()
                    stdout(value.toInt().toString())
                }

                OpCode.HALT -> {
                    return VmResult(exitCode = 0)
                }

                OpCode.ADD -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(a + b))
                }

                OpCode.SUB -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(a - b))
                }

                OpCode.MUL -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(a * b))
                }

                OpCode.DIV -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    if (b == 0L) error("Division by zero")
                    push(Value.Int(a / b))
                }

                OpCode.MOD -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    if (b == 0L) error("Modulo by zero")
                    push(Value.Int(a % b))
                }

                OpCode.NEG -> {
                    val a = pop().toInt()
                    push(Value.Int(-a))
                }

                OpCode.PUSH_TRUE -> push(Value.Bool(true))
                OpCode.PUSH_FALSE -> push(Value.Bool(false))

                OpCode.CMP_EQ -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(if (a == b) 1 else 0))
                }

                OpCode.CMP_NE -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(if (a != b) 1 else 0))
                }

                OpCode.CMP_LT -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(if (a < b) 1 else 0))
                }

                OpCode.CMP_LE -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(if (a <= b) 1 else 0))
                }

                OpCode.CMP_GT -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(if (a > b) 1 else 0))
                }

                OpCode.CMP_GE -> {
                    val b = pop().toInt()
                    val a = pop().toInt()
                    push(Value.Int(if (a >= b) 1 else 0))
                }

                OpCode.AND -> {
                    val b = pop().toBool()
                    val a = pop().toBool()
                    push(Value.Int(if (a && b) 1 else 0))
                }

                OpCode.OR -> {
                    val b = pop().toBool()
                    val a = pop().toBool()
                    push(Value.Int(if (a || b) 1 else 0))
                }

                OpCode.NOT -> {
                    val a = pop().toBool()
                    push(Value.Int(if (!a) 1 else 0))
                }

                else -> error("Unimplemented opcode: ${instr.op}")
            }

            ip++
        }

        return VmResult(exitCode = 1)
    }
}
