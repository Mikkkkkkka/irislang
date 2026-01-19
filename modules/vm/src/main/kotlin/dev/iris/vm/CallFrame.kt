package dev.iris.vm

/**
 * Represents a single function call on the call stack.
 */
data class CallFrame(
    val funcIndex: Int,           // Index of the function being executed
    val returnIp: Int,            // Instruction pointer to return to
    val locals: Array<Value>,     // Local variables for this frame
    val basePointer: Int          // Base of this frame on operand stack
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallFrame) return false
        if (funcIndex != other.funcIndex) return false
        if (returnIp != other.returnIp) return false
        if (basePointer != other.basePointer) return false
        if (!locals.contentEquals(other.locals)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = funcIndex
        result = 31 * result + returnIp
        result = 31 * result + basePointer
        result = 31 * result + locals.contentHashCode()
        return result
    }
}
