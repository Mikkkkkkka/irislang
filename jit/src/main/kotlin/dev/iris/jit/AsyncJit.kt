package dev.iris.jit

import dev.iris.core.bytecode.BytecodeProgram
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Async JIT scaffold:
 * - VM thread stays single-threaded.
 * - Compilation runs on Dispatchers.Default.
 * - Result is published atomically; VM can keep interpreting until it's ready.
 *
 * This class does NOT generate native code yet — it demonstrates the *protocol*.
 */
class AsyncJit(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    data class Compiled(val id: String, val original: BytecodeProgram)

    private val jobs = ConcurrentHashMap<String, Deferred<Compiled>>()
    private val ready = ConcurrentHashMap<String, AtomicReference<Compiled?>>()

    fun getIfReady(key: String): Compiled? = ready[key]?.get()

    fun ensureCompilation(key: String, program: BytecodeProgram) {
        ready.computeIfAbsent(key) { AtomicReference(null) }
        jobs.computeIfAbsent(key) {
            scope.async {
                // Simulate compile work:
                delay(10)
                Compiled(id = "jit:$key", original = program)
            }.also { deferred ->
                scope.launch {
                    runCatching { deferred.await() }.onSuccess { compiled ->
                        ready[key]?.set(compiled)
                    }
                }
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
