package dev.iris.core

data class Span(
    val start: Int,
    val end: Int
) {
    override fun toString() = "[$start..$end]"
}
