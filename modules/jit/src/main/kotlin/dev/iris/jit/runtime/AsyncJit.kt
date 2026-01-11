package dev.iris.jit.runtime

import dev.iris.jit.api.CompiledFunction
import dev.iris.jit.api.JitCompiler
import dev.iris.jit.api.JitInstall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel

/**
 * Orchestrates async compilation jobs and publishes compiled functions to the VM-visible table.
 * VM stays single-threaded; compilation runs on Dispatchers.Default.
 */
class AsyncJit(
    private val compiler: JitCompiler,
    funcCount: Int,
    private val events: JitEventSink = NoopJitEventSink,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : JitInstall {

    private val state = JitStateTable(funcCount)

    fun state(funcIndex: Int): JitState = state.state(funcIndex)

    fun compiled(funcIndex: Int): CompiledFunction? = state.compiled(funcIndex)

    fun ensureCompilation(funcIndex: Int): Deferred<CompiledFunction>? =
        state.tryStartCompilation(funcIndex) {
            scope.async {
                events.emit(JitEvent.CompilationStarted(funcIndex))
                try {
                    compiler.compile(funcIndex).also {
                        events.emit(JitEvent.CompilationSucceeded(funcIndex))
                    }
                } catch (t: Throwable) {
                    events.emit(JitEvent.CompilationFailed(funcIndex, t))
                    throw t
                }
            }
        }

    override fun installCompiledFunction(funcIndex: Int, fn: CompiledFunction) {
        state.publishReady(funcIndex, fn)
    }

    override fun markFailed(funcIndex: Int, cause: Throwable?) {
        state.markFailed(funcIndex, cause)
    }

    fun close() {
        scope.cancel()
    }
}
