package dev.iris.jit

import dev.iris.jit.runtime.AsyncJit
import dev.iris.jit.runtime.JitEvent
import dev.iris.jit.runtime.JitEventSink
import dev.iris.jit.support.MockJitCompiler
import dev.iris.jit.support.SimpleCompiledFunction
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AsyncJitEventsTest {

    @Test
    fun `emits start and success events`() = runBlocking {
        val events = mutableListOf<JitEvent>()
        val sink = JitEventSink { events += it }
        val compiler = MockJitCompiler { _ -> SimpleCompiledFunction { } }

        val jit = AsyncJit(compiler, funcCount = 1, events = sink)
        val job = jit.ensureCompilation(0)
        job?.await()

        assertEquals(
            listOf(
                JitEvent.CompilationStarted(0),
                JitEvent.CompilationSucceeded(0)
            ),
            events
        )
        jit.close()
    }

    @Test
    fun `emits start and failed events`() = runBlocking {
        val events = mutableListOf<JitEvent>()
        val sink = JitEventSink { events += it }
        val compiler = MockJitCompiler { _ -> throw IllegalStateException("boom") }

        val jit = AsyncJit(compiler, funcCount = 1, events = sink)
        jit.ensureCompilation(0)

        // let coroutine finish
        delay(10)

        assertEquals(2, events.size)
        val start = events[0] as JitEvent.CompilationStarted
        assertEquals(0, start.funcIndex)
        val fail = events[1]
        assertIs<JitEvent.CompilationFailed>(fail)
        assertEquals(0, fail.funcIndex)
        assertEquals("boom", fail.cause.message)
        jit.close()
    }
}
