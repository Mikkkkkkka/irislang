package dev.iris.compiler

import dev.iris.compiler.codegen.DeclCompiler
import dev.iris.core.Diagnostic
import dev.iris.core.ast.Program
import dev.iris.core.bytecode.BytecodeProgram

object Compiler {

    fun compile(ast: Program): CompileResult {
        val diags = mutableListOf<Diagnostic>()

        return try {
            val ctx = CompilerContext()
            val declCompiler = DeclCompiler(ctx)
            declCompiler.compile(ast)
            ctx.emitter.patchJumps()

            CompileResult(
                program = BytecodeProgram(
                    instructions = ctx.emitter.getInstructions(),
                    functions = ctx.functions.toList()
                ),
                diagnostics = diags
            )
        } catch (e: CompilerException) {
            diags += Diagnostic(e.message ?: "Unknown compiler error")
            CompileResult(program = null, diagnostics = diags)
        }
    }
}
