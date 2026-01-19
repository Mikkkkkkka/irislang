package dev.iris.compiler.emit

class Label {
    var address: Int = -1
    val pending = mutableListOf<Int>()

    fun isResolved(): Boolean = address >= 0
}
