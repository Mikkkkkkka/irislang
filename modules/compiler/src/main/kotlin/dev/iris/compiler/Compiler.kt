package dev.iris.compiler

import dev.iris.compiler.codegen.DeclCompiler
import dev.iris.core.Diagnostic
import dev.iris.core.ast.Decl
import dev.iris.core.ast.Program
import dev.iris.core.bytecode.BytecodeProgram

object Compiler {

    fun compile(ast: Program): CompileResult {
        val diags = mutableListOf<Diagnostic>()

        return try {
            validateEntryPoint(ast)
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

    private fun validateEntryPoint(ast: Program) {
        if (ast.statements.isNotEmpty()) {
            throw TopLevelStatementsNotAllowedException()
        }

        val mainDecl = ast.declarations.firstOrNull { decl ->
            when (decl) {
                is Decl.Function -> decl.name == "главная"
                is Decl.Procedure -> decl.name == "главная"
                else -> false
            }
        } ?: throw MissingMainFunctionException()

        val paramCount = when (mainDecl) {
            is Decl.Function -> mainDecl.params.size
            is Decl.Procedure -> mainDecl.params.size
            else -> 0
        }

        if (paramCount != 0) {
            throw InvalidMainSignatureException("главная must have no parameters")
        }
    }
}
