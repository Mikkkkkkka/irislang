package dev.iris.lexer

import dev.iris.core.Diagnostic

data class LexResult (
    val tokens: List<Token>,
    val diagnostics: List<Diagnostic>,
)