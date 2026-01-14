package dev.iris.vm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ValueTest {
    @Test
    fun `int values with same value are equal`() {
        val v1 = Value.Int(42)
        val v2 = Value.Int(42)
        assertEquals(v1, v2)
    }

    @Test
    fun `int values with different values are not equal`() {
        val v1 = Value.Int(42)
        val v2 = Value.Int(43)
        assertNotEquals(v1, v2)
    }

    @Test
    fun `bool values work correctly`() {
        val vTrue = Value.Bool(true)
        val vFalse = Value.Bool(false)
        assertEquals(Value.Bool(true), vTrue)
        assertNotEquals(vTrue, vFalse)
    }

    @Test
    fun `heap references store addresses`() {
        val ref1 = Value.HeapRef(0x1000)
        val ref2 = Value.HeapRef(0x1000)
        assertEquals(ref1, ref2)
    }
}
