package dev.iris.jit.pipeline

import dev.iris.jit.api.BytecodeProvider

/**
 * Converts VM bytecode into a lowered IR understood by the emitter.
 * For now it simply forwards instructions and metadata 1:1.
 */
class BytecodeLowering(
    private val provider: BytecodeProvider
) {
    fun lower(funcIndex: Int): LoweredFunction {
        val meta = provider.getFunctionMeta(funcIndex)
        val instrs = provider.getFunctionBytecode(funcIndex)
        return LoweredFunction(funcIndex, meta, instrs)
    }
}
