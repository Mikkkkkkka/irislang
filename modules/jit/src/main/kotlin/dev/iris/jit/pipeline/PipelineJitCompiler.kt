package dev.iris.jit.pipeline

import dev.iris.jit.api.CompiledFunction
import dev.iris.jit.api.JitCompiler

/**
 * JITCompiler implementation that wires lowering + emitting stages.
 */
class PipelineJitCompiler(
    private val lowering: BytecodeLowering,
    private val emitter: CodeEmitter
) : JitCompiler {
    override fun compile(funcIndex: Int): CompiledFunction {
        val lowered = lowering.lower(funcIndex)
        return emitter.emit(lowered)
    }
}
