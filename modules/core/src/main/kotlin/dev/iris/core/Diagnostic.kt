package dev.iris.core

data class Diagnostic(
    val message: String,
    val span: Span? = null,
    val severity: Severity = Severity.ERROR
) {
    enum class Severity { INFO, WARNING, ERROR }
}
