package dev.iris.jit

import dev.iris.jit.api.CompiledFunction
import dev.iris.jit.runtime.JitState
import dev.iris.jit.runtime.JitStateTable
import dev.iris.jit.support.SimpleCompiledFunction
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class JitStateTableTest {

    @Test
    fun `transitions to READY on successful job completion`() {
        val table = JitStateTable(funcCount = 1)
        val deferred = CompletableDeferred<CompiledFunction>()

        val job = table.tryStartCompilation(0) { deferred }
        assertSame(deferred, job)
        assertEquals(JitState.COMPILING, table.state(0))
        assertNull(table.compiled(0))

        val compiled = SimpleCompiledFunction { }
        deferred.complete(compiled)

        // Completion handler should publish the result immediately
        assertEquals(JitState.READY, table.state(0))
        assertSame(compiled, table.compiled(0))

        // Further attempts should be ignored in READY
        val second = table.tryStartCompilation(0) { CompletableDeferred() }
        assertNull(second)
    }

    @Test
    fun `transitions to FAILED on job failure`() {
        val table = JitStateTable(funcCount = 1)
        val deferred = CompletableDeferred<CompiledFunction>()

        table.tryStartCompilation(0) { deferred }
        deferred.completeExceptionally(IllegalStateException("boom"))

        assertEquals(JitState.FAILED, table.state(0))
        assertNull(table.compiled(0))

        // No retries after FAILED
        val second = table.tryStartCompilation(0) { CompletableDeferred() }
        assertNull(second)
    }
}
