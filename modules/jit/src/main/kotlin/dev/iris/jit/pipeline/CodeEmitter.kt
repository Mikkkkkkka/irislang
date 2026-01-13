package dev.iris.jit.pipeline

import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.CompiledFunction
import dev.iris.jit.runtime.StackUnderflowException
import dev.iris.jit.support.RunnableCompiledFunction

/**
 * Emits a runnable artifact from lowered IR.
 */
interface CodeEmitter {
    fun emit(function: LoweredFunction): CompiledFunction
}

/**
 * Baseline emitter that produces a per-function "super-interpreter"
 * to avoid fetch/decode overhead for that function. Supports current opcode set.
 */
class BaselineCodeEmitter(
    private val printSink: (Long) -> Unit = { println(it) }
) : CodeEmitter {
    override fun emit(function: LoweredFunction): CompiledFunction {
        val instrs = function.instructions
        return RunnableCompiledFunction {
            val stack = ArrayDeque<Long>()
            var ip = 0
            while (ip in instrs.indices) {
                val ins = instrs[ip]
                when (ins.op) {
                    OpCode.PUSH_I64 -> {
                        val value = ins.operand ?: error("Missing operand for PUSH_I64")
                        stack.addLast(value)
                    }
                    OpCode.PRINT_I64 -> {
                        val value = stack.removeLastOrNull() ?: throw StackUnderflowException("PRINT_I64")
                        printSink(value)
                    }
                    OpCode.HALT -> return@RunnableCompiledFunction
                }
                ip++
            }
        }
    }
}
