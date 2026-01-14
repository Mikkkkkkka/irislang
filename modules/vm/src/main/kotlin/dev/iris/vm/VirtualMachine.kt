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

        // Initialize with a main call frame if functions are defined
        if (program.functions.isNotEmpty()) {
            val mainFunc = program.functions.find { it.startIp == 0 }
            if (mainFunc != null) {
                val frame = CallFrame(
                    funcIndex = 0,
                    returnIp = -1,  // Sentinel value for main function
                    locals = Array(mainFunc.localCount) { Value.Int(0) },
                    basePointer = 0
                )
                callStack.addLast(frame)
            }
        }

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

                OpCode.JMP -> {
                    val target = instr.operand ?: error("Missing operand for JMP")
                    ip = target.toInt() - 1  // -1 because ip++ at end of loop
                }

                OpCode.JMP_IF_FALSE -> {
                    val target = instr.operand ?: error("Missing operand for JMP_IF_FALSE")
                    val condition = pop().toBool()
                    if (!condition) {
                        ip = target.toInt() - 1  // -1 because ip++ at end of loop
                    }
                }

                OpCode.JMP_IF_TRUE -> {
                    val target = instr.operand ?: error("Missing operand for JMP_IF_TRUE")
                    val condition = pop().toBool()
                    if (condition) {
                        ip = target.toInt() - 1  // -1 because ip++ at end of loop
                    }
                }

                OpCode.LOAD_GLOBAL -> {
                    val index = instr.operand?.toInt() ?: error("Missing operand for LOAD_GLOBAL")
                    // Ensure globals list is large enough
                    while (globals.size <= index) {
                        globals.add(Value.Int(0))
                    }
                    push(globals[index])
                }

                OpCode.STORE_GLOBAL -> {
                    val index = instr.operand?.toInt() ?: error("Missing operand for STORE_GLOBAL")
                    val value = pop()
                    // Ensure globals list is large enough
                    while (globals.size <= index) {
                        globals.add(Value.Int(0))
                    }
                    globals[index] = value
                }

                OpCode.LOAD_LOCAL -> {
                    val index = instr.operand?.toInt() ?: error("Missing operand for LOAD_LOCAL")
                    if (callStack.isEmpty()) error("No call frame for LOAD_LOCAL")
                    val frame = callStack.last()
                    if (index >= frame.locals.size) error("Local index out of bounds: $index")
                    push(frame.locals[index])
                }

                OpCode.STORE_LOCAL -> {
                    val index = instr.operand?.toInt() ?: error("Missing operand for STORE_LOCAL")
                    if (callStack.isEmpty()) error("No call frame for STORE_LOCAL")
                    val frame = callStack.last()
                    if (index >= frame.locals.size) error("Local index out of bounds: $index")
                    frame.locals[index] = pop()
                }

                OpCode.CALL -> {
                    val funcIndex = instr.operand?.toInt() ?: error("Missing operand for CALL")
                    if (funcIndex >= program.functions.size) error("Function index out of bounds: $funcIndex")

                    val funcInfo = program.functions[funcIndex]
                    val frame = CallFrame(
                        funcIndex = funcIndex,
                        returnIp = ip + 1,  // Return to next instruction after CALL
                        locals = Array(funcInfo.localCount) { Value.Int(0) },
                        basePointer = stack.size
                    )
                    callStack.addLast(frame)
                    ip = funcInfo.startIp - 1  // -1 because ip++ at end of loop
                }

                OpCode.RET -> {
                    if (callStack.isEmpty()) error("No call frame to return from")
                    val returnValue = pop()
                    val frame = callStack.removeLast()
                    if (frame.returnIp == -1) {
                        // Returning from main, halt execution
                        push(returnValue)
                        return VmResult(exitCode = 0)
                    }
                    ip = frame.returnIp - 1  // -1 because ip++ at end of loop
                    push(returnValue)
                }

                OpCode.RET_VOID -> {
                    if (callStack.isEmpty()) error("No call frame to return from")
                    val frame = callStack.removeLast()
                    if (frame.returnIp == -1) {
                        // Returning from main, halt execution
                        return VmResult(exitCode = 0)
                    }
                    ip = frame.returnIp - 1  // -1 because ip++ at end of loop
                }

                else -> error("Unimplemented opcode: ${instr.op}")
            }

            ip++
        }

        return VmResult(exitCode = 1)
    }
}
