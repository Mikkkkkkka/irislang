package dev.iris.parser

import dev.iris.core.Diagnostic
import dev.iris.core.ast.Program

data class ParseResult(
    val program: Program?,
    val diagnostics: List<Diagnostic>
)
