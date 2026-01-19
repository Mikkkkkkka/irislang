package dev.iris.vm

sealed class Value {
    data class Int(val value: Long) : Value()
    data class Bool(val value: Boolean) : Value()
    data class HeapRef(val address: Long) : Value()

    fun toInt(): Long = when (this) {
        is Int -> value
        is Bool -> if (value) 1 else 0
        is HeapRef -> error("Cannot convert HeapRef to Int")
    }

    fun toBool(): Boolean = when (this) {
        is Bool -> value
        is Int -> value != 0L
        is HeapRef -> true  // non-null reference is truthy
    }
}
