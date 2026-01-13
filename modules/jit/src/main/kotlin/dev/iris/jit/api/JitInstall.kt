package dev.iris.jit.api

/**
 * Publication/failure hooks so VM can install compiled code safely.
 */
interface JitInstall {
    fun installCompiledFunction(funcIndex: Int, fn: CompiledFunction)
    fun markFailed(funcIndex: Int, cause: Throwable? = null)
}
