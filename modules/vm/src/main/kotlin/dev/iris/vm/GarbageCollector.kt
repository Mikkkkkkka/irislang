package dev.iris.vm

/**
 * Mark-sweep garbage collector.
 *
 * Algorithm:
 * 1. Mark phase: traverse from roots, mark all reachable objects
 * 2. Sweep phase: free all unmarked objects
 */
class GarbageCollector(private val heap: Heap) {

    /**
     * Collect garbage.
     * @param roots All root values (stack, locals, globals)
     */
    fun collect(roots: List<Value>) {
        // Mark phase
        clearMarks()
        roots.forEach { markValue(it) }

        // Sweep phase
        sweep()
    }

    private fun clearMarks() {
        heap.allObjects().forEach { it.gcMarked = false }
    }

    private fun markValue(value: Value) {
        if (value !is Value.HeapRef) return

        val obj = try {
            heap.get(value.address)
        } catch (e: IllegalStateException) {
            return  // Already freed or invalid
        }

        if (obj.gcMarked) return  // Already marked

        obj.gcMarked = true

        // Recursively mark referenced objects
        when (obj) {
            is HeapObject.Array -> {
                obj.elements.forEach { markValue(it) }
            }
            is HeapObject.Struct -> {
                obj.fields.forEach { markValue(it) }
            }
        }
    }

    private fun sweep() {
        val toFree = heap.allAddresses().filter { address ->
            !heap.get(address).gcMarked
        }
        toFree.forEach { heap.free(it) }
    }
}
