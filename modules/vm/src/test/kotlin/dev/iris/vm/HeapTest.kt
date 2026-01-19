package dev.iris.vm

import kotlin.test.Test
import kotlin.test.*

class HeapTest {
    @Test
    fun `can allocate array and get reference`() {
        val heap = Heap()
        val ref = heap.allocArray(size = 10)
        assertEquals(0L, ref)  // First allocation gets address 0
    }

    @Test
    fun `can allocate multiple objects`() {
        val heap = Heap()
        val ref1 = heap.allocArray(size = 5)
        val ref2 = heap.allocArray(size = 3)

        assertNotEquals(ref1, ref2)
    }

    @Test
    fun `can get allocated object`() {
        val heap = Heap()
        val ref = heap.allocArray(size = 7)
        val obj = heap.get(ref)

        assertIs<HeapObject.Array>(obj)
        assertEquals(7, obj.size)
    }

    @Test
    fun `can allocate struct`() {
        val heap = Heap()
        val ref = heap.allocStruct(typeIndex = 0, fieldCount = 3)
        val obj = heap.get(ref)

        assertIs<HeapObject.Struct>(obj)
        assertEquals(0, obj.typeIndex)
        assertEquals(3, obj.fields.size)
    }

    @Test
    fun `throws on invalid reference`() {
        val heap = Heap()
        assertFailsWith<IllegalStateException> {
            heap.get(999)
        }
    }

    @Test
    fun `allocatedCount tracks allocations`() {
        val heap = Heap()
        assertEquals(0, heap.allocatedCount)

        heap.allocArray(5)
        assertEquals(1, heap.allocatedCount)

        heap.allocStruct(0, 2)
        assertEquals(2, heap.allocatedCount)
    }
}
