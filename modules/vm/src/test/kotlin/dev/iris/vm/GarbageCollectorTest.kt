package dev.iris.vm

import kotlin.test.Test
import kotlin.test.*

class GarbageCollectorTest {
    @Test
    fun `gc does not collect reachable objects`() {
        val heap = Heap()
        val ref1 = heap.allocArray(5)
        val ref2 = heap.allocArray(3)

        val roots = listOf(Value.HeapRef(ref1), Value.HeapRef(ref2))
        val gc = GarbageCollector(heap)

        val initialCount = heap.allocatedCount
        gc.collect(roots)

        assertEquals(initialCount, heap.allocatedCount)
        assertNotNull(heap.get(ref1))
        assertNotNull(heap.get(ref2))
    }

    @Test
    fun `gc collects unreachable objects`() {
        val heap = Heap()
        val ref1 = heap.allocArray(5)
        val ref2 = heap.allocArray(3)

        // Only ref1 is reachable
        val roots = listOf(Value.HeapRef(ref1))
        val gc = GarbageCollector(heap)

        gc.collect(roots)

        assertEquals(1, heap.allocatedCount)
        assertNotNull(heap.get(ref1))
        assertFailsWith<IllegalStateException> {
            heap.get(ref2)
        }
    }

    @Test
    fun `gc follows references in arrays`() {
        val heap = Heap()
        val innerRef = heap.allocArray(2)
        val outerRef = heap.allocArray(1)

        val outer = heap.get(outerRef) as HeapObject.Array
        outer.elements[0] = Value.HeapRef(innerRef)

        // Only outerRef is root, but innerRef should be reachable
        val roots = listOf(Value.HeapRef(outerRef))
        val gc = GarbageCollector(heap)

        gc.collect(roots)

        assertEquals(2, heap.allocatedCount)
        assertNotNull(heap.get(outerRef))
        assertNotNull(heap.get(innerRef))
    }

    @Test
    fun `gc follows references in structs`() {
        val heap = Heap()
        val innerRef = heap.allocArray(1)
        val structRef = heap.allocStruct(typeIndex = 0, fieldCount = 2)

        val struct = heap.get(structRef) as HeapObject.Struct
        struct.fields[1] = Value.HeapRef(innerRef)

        val roots = listOf(Value.HeapRef(structRef))
        val gc = GarbageCollector(heap)

        gc.collect(roots)

        assertEquals(2, heap.allocatedCount)
    }

    @Test
    fun `gc collects all garbage when no roots`() {
        val heap = Heap()
        heap.allocArray(5)
        heap.allocArray(3)
        heap.allocStruct(0, 2)

        assertEquals(3, heap.allocatedCount)

        val gc = GarbageCollector(heap)
        gc.collect(emptyList())

        assertEquals(0, heap.allocatedCount)
    }
}
