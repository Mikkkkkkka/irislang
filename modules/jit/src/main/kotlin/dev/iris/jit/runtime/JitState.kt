package dev.iris.jit.runtime

import dev.iris.jit.api.CompiledFunction
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicReferenceArray

enum class JitState { NOT_COMPILED, COMPILING, READY, FAILED }

/**
 * Thread-safe holder for async JIT state per function.
 * Follows the NOT_COMPILED -> COMPILING -> READY/FAILED lifecycle.
 */
class JitStateTable(funcCount: Int) {

    private val states = AtomicIntegerArray(funcCount)
    private val compiled = AtomicReferenceArray<CompiledFunction?>(funcCount)
    private val jobs = ConcurrentHashMap<Int, Deferred<CompiledFunction>>()

    fun state(funcIndex: Int): JitState = JitState.values()[states.get(funcIndex)]

    fun compiled(funcIndex: Int): CompiledFunction? = compiled.get(funcIndex)

    fun job(funcIndex: Int): Deferred<CompiledFunction>? = jobs[funcIndex]

    fun publishReady(funcIndex: Int, fn: CompiledFunction) {
        compiled.set(funcIndex, fn)
        states.set(funcIndex, JitState.READY.ordinal)
    }

    fun markFailed(funcIndex: Int, cause: Throwable? = null) {
        states.set(funcIndex, JitState.FAILED.ordinal)
    }

    /**
     * Start compilation once for a function. If already compiling/ready/failed, returns the existing job or null.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun tryStartCompilation(
        funcIndex: Int,
        launcher: () -> Deferred<CompiledFunction>
    ): Deferred<CompiledFunction>? {
        val current = state(funcIndex)
        if (current == JitState.READY || current == JitState.FAILED) return null
        if (current == JitState.COMPILING) return jobs[funcIndex]

        val started = states.compareAndSet(
            funcIndex,
            JitState.NOT_COMPILED.ordinal,
            JitState.COMPILING.ordinal
        )
        if (!started) return jobs[funcIndex]

        val job = launcher()
        jobs[funcIndex] = job
        job.invokeOnCompletion { err ->
            if (err == null) {
                publishReady(funcIndex, job.getCompleted())
            } else {
                markFailed(funcIndex, err)
            }
        }
        return job
    }
}
