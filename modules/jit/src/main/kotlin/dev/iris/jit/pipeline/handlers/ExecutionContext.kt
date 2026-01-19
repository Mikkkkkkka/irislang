package dev.iris.jit.pipeline.handlers

class ExecutionContext(
    val stack: ArrayDeque<Long>,
    val locals: LongArray,
    val globals: LongArray,
    var ip: Int,
    val printSink: (String) -> Unit
) {
    var halted: Boolean = false
    var returnValue: Long? = null
}
