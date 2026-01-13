package dev.iris.core.ast

data class Program(
    val declarations: List<Decl>,
    val statements: List<Stmt>
)
