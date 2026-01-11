package dev.iris.parser

import dev.iris.core.Diagnostic
import dev.iris.core.Span
import dev.iris.core.ast.Program
import dev.iris.core.ast.Stmt

/**
 * Tiny line-based parser for the scaffold:
 *   print 123
 *
 * Replace with a real lexer+parser later.
 */
object Parser {

    fun parse(source: String): ParseResult {
        val diags = mutableListOf<Diagnostic>()
        val stmts = mutableListOf<dev.iris.core.ast.Stmt>()

        var offset = 0
        val lines = source.split("\n")
        for ((i, raw) in lines.withIndex()) {
            val line = raw.trim()
            val lineStart = offset
            offset += raw.length + 1

            if (line.isBlank()) continue

            val parts = line.split(Regex("\\s+"))
            if (parts.size == 2 && parts[0] == "print") {
                val n = parts[1].toLongOrNull()
                if (n == null) {
                    diags += Diagnostic(
                        message = "Line ${i + 1}: expected integer after 'print'",
                        span = Span(lineStart, lineStart + raw.length)
                    )
                } else {
                    stmts += Stmt.PrintInt(n, span = Span(lineStart, lineStart + raw.length))
                }
            } else {
                diags += Diagnostic(
                    message = "Line ${i + 1}: expected 'print <int>'",
                    span = Span(lineStart, lineStart + raw.length)
                )
            }
        }

        return ParseResult(
            program = if (diags.any { it.severity == Diagnostic.Severity.ERROR }) null else Program(stmts),
            diagnostics = diags
        )
    }
}
