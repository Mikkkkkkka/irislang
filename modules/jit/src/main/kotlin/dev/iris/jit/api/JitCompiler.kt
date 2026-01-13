package dev.iris.jit.api

/**
 * JIT compiler entrypoint: translate one function.
 */
interface JitCompiler {
    fun compile(funcIndex: Int): CompiledFunction
}
