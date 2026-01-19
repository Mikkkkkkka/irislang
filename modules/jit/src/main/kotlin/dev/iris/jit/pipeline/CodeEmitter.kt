package dev.iris.jit.pipeline

import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.CompiledFunction
import dev.iris.jit.pipeline.handlers.ExecutionContext
import dev.iris.jit.pipeline.handlers.HandlerRegistry
import dev.iris.jit.support.RunnableCompiledFunction

interface CodeEmitter {
    fun emit(function: LoweredFunction): CompiledFunction
}

class BaselineCodeEmitter(
    private val printSink: (String) -> Unit = ::println,
    private val registry: HandlerRegistry = HandlerRegistry()
) : CodeEmitter {
    override fun emit(function: LoweredFunction): CompiledFunction {
        val instrs = function.instructions
        val meta = function.meta

        return RunnableCompiledFunction { _ ->
            val ctx = ExecutionContext(
                stack = ArrayDeque(),
                locals = LongArray(meta.localsCount),
                globals = LongArray(meta.globalsCount),
                ip = 0,
                printSink = printSink
            )

            while (ctx.ip in instrs.indices && !ctx.halted) {
                val ins = instrs[ctx.ip]
                if (ins.op == OpCode.HALT) return@RunnableCompiledFunction
                registry.get(ins.op).handle(ctx, ins)
                ctx.ip++
            }
        }
    }
}
