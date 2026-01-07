package dev.iris.compiler

import dev.iris.core.Diagnostic
import dev.iris.core.bytecode.BytecodeProgram

data class CompileResult(
    val program: BytecodeProgram?,
    val diagnostics: List<Diagnostic>
)
