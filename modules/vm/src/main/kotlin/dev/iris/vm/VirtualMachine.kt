package dev.iris.vm

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.OpCode

/**
 * Minimal stack VM for the scaffold.
 * Keep this single-threaded (per your plan), but you can run async JIT compilation elsewhere.
 */
class VirtualMachine {

    private val stack = ArrayDeque<Long>()

    fun run(program: BytecodeProgram, stdout: (String) -> Unit = ::println): VmResult {
        val code = program.instructions
        var ip = 0
        while (ip in code.indices) {
            val ins = code[ip]
            when (ins.op) {
                OpCode.PUSH_I64 -> stack.addLast(ins.operand ?: error("Missing operand for PUSH_I64"))
                OpCode.PRINT_I64 -> stdout(stack.removeLast().toString())
                OpCode.HALT -> return VmResult(exitCode = 0)
                else -> error("Unimplemented opcode: ${ins.op}")
            }
            ip++
        }
        return VmResult(exitCode = 1)
    }
}
