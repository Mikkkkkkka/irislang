package dev.iris.jit.runtime

sealed interface JitEvent {
    data class CompilationStarted(val funcIndex: Int) : JitEvent
    data class CompilationSucceeded(val funcIndex: Int) : JitEvent
    data class CompilationFailed(val funcIndex: Int, val cause: Throwable) : JitEvent
}

fun interface JitEventSink {
    fun emit(event: JitEvent)
}

object NoopJitEventSink : JitEventSink {
    override fun emit(event: JitEvent) = Unit
}
