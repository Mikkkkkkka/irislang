package dev.iris.core.ast

import dev.iris.core.ast.Stmt

data class Program(
    val statements: List<Stmt>
)
