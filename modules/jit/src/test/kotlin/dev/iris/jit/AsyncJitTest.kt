package dev.iris.jit

import dev.iris.jit.runtime.AsyncJit
import dev.iris.jit.runtime.JitState
import dev.iris.jit.support.MockJitCompiler
import dev.iris.jit.support.SimpleCompiledFunction
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AsyncJitTest {

    @Test
    fun `deduplicates compilation and publishes result`(): Unit = runBlocking {
        var compiledCount = 0
        val compiler = MockJitCompiler { _ ->
            compiledCount++
            SimpleCompiledFunction { }
        }
        val jit = AsyncJit(compiler, funcCount = 1)

        // Trigger two requests for the same funcIndex
        val job1 = jit.ensureCompilation(0)
        val job2 = jit.ensureCompilation(0)

        assertEquals(job1, job2, "Expected same Deferred for deduplicated compile")

        // Wait a moment for completion
        job1?.await()
        assertEquals(1, compiledCount, "compile should run once")
        assertNotNull(jit.compiled(0), "compiled function should be published")
        jit.close()
    }

    @Test
    fun `marks failed and stops retrying`() = runBlocking {
        var attempts = 0
        val compiler = MockJitCompiler { _ ->
            attempts++
            throw IllegalStateException("boom")
        }
        val jit = AsyncJit(compiler, funcCount = 1)

        jit.ensureCompilation(0)
        // Allow coroutine to complete
        delay(10)

        assertEquals(1, attempts, "compile should run once")
        assertEquals(JitState.FAILED, jit.state(0), "state should be FAILED")
        assertNull(jit.compiled(0), "no compiled function should be published")

        // Subsequent requests should not start new jobs
        jit.ensureCompilation(0)
        delay(10)
        assertEquals(1, attempts, "no retries after FAILED")
        jit.close()
    }
}
