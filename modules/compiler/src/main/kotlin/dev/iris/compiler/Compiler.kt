package dev.iris.compiler

import dev.iris.core.Diagnostic
import dev.iris.core.ast.PrintInt
import dev.iris.core.ast.Program
import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode

object Compiler {

    fun compile(ast: Program): CompileResult {
        val diags = mutableListOf<Diagnostic>()
        val out = mutableListOf<Instr>()

        for (stmt in ast.statements) {
            when (stmt) {
                is PrintInt -> {
                    out += Instr(OpCode.PUSH_I64, stmt.value)
                    out += Instr(OpCode.PRINT_I64)
                }
                else -> diags += Diagnostic("Unsupported statement: ${stmt::class.simpleName}")
            }
        }

        out += Instr(OpCode.HALT)
        return CompileResult(
            program = if (diags.any { it.severity == Diagnostic.Severity.ERROR }) null else BytecodeProgram(out),
            diagnostics = diags
        )
    }
}
