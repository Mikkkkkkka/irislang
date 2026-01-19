package dev.iris.vm

import kotlin.test.Test
import kotlin.test.assertEquals

class CallFrameTest {
    @Test
    fun `call frame stores function metadata`() {
        val locals = arrayOf<Value>(Value.Int(10), Value.Int(20))
        val frame = CallFrame(
            funcIndex = 0,
            returnIp = 42,
            locals = locals,
            basePointer = 5
        )

        assertEquals(0, frame.funcIndex)
        assertEquals(42, frame.returnIp)
        assertEquals(5, frame.basePointer)
        assertEquals(Value.Int(10), frame.locals[0])
    }

    @Test
    fun `can update local variables`() {
        val locals = arrayOf<Value>(Value.Int(0), Value.Int(0))
        val frame = CallFrame(0, 0, locals, 0)

        frame.locals[0] = Value.Int(99)
        assertEquals(Value.Int(99), frame.locals[0])
    }
}
