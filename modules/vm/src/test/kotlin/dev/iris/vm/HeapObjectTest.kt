package dev.iris.vm

import kotlin.test.Test
import kotlin.test.*

class HeapObjectTest {
    @Test
    fun `array object stores size and elements`() {
        val arr = HeapObject.Array(size = 5)
        assertEquals(5, arr.size)
        assertEquals(5, arr.elements.size)
        assertTrue(arr.elements.all { it is Value.Int && it.value == 0L })
    }

    @Test
    fun `array elements can be read and written`() {
        val arr = HeapObject.Array(size = 3)
        arr.elements[0] = Value.Int(10)
        arr.elements[2] = Value.Bool(true)

        assertEquals(Value.Int(10), arr.elements[0])
        assertEquals(Value.Bool(true), arr.elements[2])
    }

    @Test
    fun `struct object stores field values`() {
        val fields = arrayOf<Value>(Value.Int(1), Value.Int(2))
        val struct = HeapObject.Struct(typeIndex = 0, fields = fields)

        assertEquals(0, struct.typeIndex)
        assertEquals(2, struct.fields.size)
        assertEquals(Value.Int(1), struct.fields[0])
    }

    @Test
    fun `gc mark flag defaults to false`() {
        val arr = HeapObject.Array(size = 1)
        assertFalse(arr.gcMarked)
    }

    @Test
    fun `gc mark flag can be set`() {
        val arr = HeapObject.Array(size = 1)
        arr.gcMarked = true
        assertTrue(arr.gcMarked)
    }
}
