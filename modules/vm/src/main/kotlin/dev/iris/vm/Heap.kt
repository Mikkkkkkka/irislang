package dev.iris.vm

/**
 * Heap memory manager.
 * Stores all heap-allocated objects (arrays, structs).
 */
class Heap {
    private val objects = mutableMapOf<Long, HeapObject>()
    private var nextAddress = 0L

    val allocatedCount: Int
        get() = objects.size

    /**
     * Allocate an array on the heap.
     * Returns a reference (address) to the array.
     */
    fun allocArray(size: Int): Long {
        val address = nextAddress++
        objects[address] = HeapObject.Array(size)
        return address
    }

    /**
     * Allocate a struct on the heap.
     * Returns a reference (address) to the struct.
     */
    fun allocStruct(typeIndex: Int, fieldCount: Int): Long {
        val address = nextAddress++
        val fields = Array<Value>(fieldCount) { Value.Int(0) }
        objects[address] = HeapObject.Struct(typeIndex, fields)
        return address
    }

    /**
     * Get object by reference.
     * Throws if reference is invalid.
     */
    fun get(ref: Long): HeapObject {
        return objects[ref] ?: error("Invalid heap reference: $ref")
    }

    /**
     * Get all allocated objects (for GC traversal).
     */
    fun allObjects(): Collection<HeapObject> = objects.values

    /**
     * Get all addresses (for GC sweep).
     */
    fun allAddresses(): Set<Long> = objects.keys

    /**
     * Remove object at address (used by GC sweep).
     */
    fun free(ref: Long) {
        objects.remove(ref)
    }
}
