package dev.iris.jit

import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.FunctionMeta
import dev.iris.jit.api.CompiledFunction
import dev.iris.jit.pipeline.BytecodeLowering
import dev.iris.jit.pipeline.CodeEmitter
import dev.iris.jit.pipeline.LoweredFunction
import dev.iris.jit.pipeline.PipelineJitCompiler
import dev.iris.jit.support.InMemoryBytecodeProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PipelineJitCompilerTest {

    @Test
    fun `runs lowering and emitter in order`() {
        val instrs = listOf(Instr(OpCode.PUSH_I64, 1), Instr(OpCode.HALT))
        val meta = FunctionMeta(arity = 0, localsCount = 0, stackLimit = 8)
        val provider = InMemoryBytecodeProvider(
            functions = listOf(instrs),
            metas = listOf(meta)
        )
        val lowering = BytecodeLowering(provider)

        var capturedLowered: LoweredFunction? = null
        val compiled = object : CompiledFunction {
            override fun execute(vm: dev.iris.vm.VirtualMachine) = Unit
        }
        val emitter = object : CodeEmitter {
            override fun emit(function: LoweredFunction): CompiledFunction {
                capturedLowered = function
                return compiled
            }
        }

        val jit = PipelineJitCompiler(lowering, emitter)
        val result = jit.compile(0)

        assertSame(compiled, result, "Emitter result should propagate")
        val lowered = capturedLowered ?: error("Lowered function not captured")
        assertEquals(meta, lowered.meta)
        assertEquals(instrs, lowered.instructions)
    }
}
