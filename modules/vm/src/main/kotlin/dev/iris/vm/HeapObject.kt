package dev.iris.vm

/**
 * Objects allocated on the heap.
 * All heap objects have a GC mark bit.
 */
sealed class HeapObject {
    var gcMarked: Boolean = false

    /**
     * Array of values.
     */
    data class Array(
        val size: Int,
        val elements: kotlin.Array<Value> = kotlin.Array(size) { Value.Int(0) }
    ) : HeapObject() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Array) return false
            if (size != other.size) return false
            if (!elements.contentEquals(other.elements)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = size
            result = 31 * result + elements.contentHashCode()
            return result
        }
    }

    /**
     * Struct with named fields (stored as indexed array).
     */
    data class Struct(
        val typeIndex: Int,  // Index into struct type table
        val fields: kotlin.Array<Value>
    ) : HeapObject() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Struct) return false
            if (typeIndex != other.typeIndex) return false
            if (!fields.contentEquals(other.fields)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = typeIndex
            result = 31 * result + fields.contentHashCode()
            return result
        }
    }
}
