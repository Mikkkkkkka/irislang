package dev.iris.compiler.codegen

import dev.iris.compiler.CompilerContext
import dev.iris.compiler.StructureInfo
import dev.iris.core.ast.Decl
import dev.iris.core.ast.Program
import dev.iris.core.ast.TypeRef
import dev.iris.core.bytecode.FunctionInfo
import dev.iris.core.bytecode.OpCode

class DeclCompiler(private val ctx: CompilerContext) {
    private val stmtCompiler = StmtCompiler(ctx)

    fun compile(program: Program) {
        registerDeclarations(program)
        compileDeclarations(program)
    }

    private fun registerDeclarations(program: Program) {
        for (decl in program.declarations) {
            when (decl) {
                is Decl.Function -> registerFunction(decl)
                is Decl.Procedure -> registerProcedure(decl)
                is Decl.Structure -> registerStructure(decl)
            }
        }
    }

    private fun registerFunction(decl: Decl.Function) {
        val symbol = ctx.symbols.defineFunction(
            name = decl.name,
            returnType = decl.returnType,
            paramCount = decl.params.size,
            returnsValue = true
        )
        while (ctx.functions.size <= symbol.index) {
            ctx.functions.add(
                FunctionInfo(
                    name = "",
                    startIp = -1,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = false
                )
            )
        }
    }

    private fun registerProcedure(decl: Decl.Procedure) {
        val symbol = ctx.symbols.defineFunction(
            name = decl.name,
            returnType = TypeRef.IntType,
            paramCount = decl.params.size,
            returnsValue = false
        )
        while (ctx.functions.size <= symbol.index) {
            ctx.functions.add(
                FunctionInfo(
                    name = "",
                    startIp = -1,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = false
                )
            )
        }
    }

    private fun registerStructure(decl: Decl.Structure) {
        val fields = decl.fields.map { it.name to it.type }
        ctx.symbols.defineStruct(decl.name, fields)
        ctx.structures.add(
            StructureInfo(
                name = decl.name,
                index = ctx.structures.size,
                fields = fields
            )
        )
    }

    private fun compileDeclarations(program: Program) {
        for (decl in program.declarations) {
            when (decl) {
                is Decl.Function -> compileFunction(decl)
                is Decl.Procedure -> compileProcedure(decl)
                is Decl.Structure -> {}
            }
        }
    }

    private fun compileFunction(decl: Decl.Function) {
        val funcSymbol = ctx.symbols.resolve(decl.name) as dev.iris.compiler.symbols.Symbol.Function
        val startIp = ctx.emitter.currentAddress()
        ctx.currentFunctionReturnsValue = true
        ctx.inFunctionBody = true

        ctx.symbols.enterScope(isFunction = true)

        for (param in decl.params) {
            ctx.symbols.defineLocal(param.name, param.type)
        }

        for (i in (decl.params.size - 1) downTo 0) {
            ctx.emitter.emit(OpCode.STORE_LOCAL, i.toLong())
        }

        stmtCompiler.compileStatements(decl.body.stmts)

        val localCount = ctx.symbols.exitScope()
        ctx.inFunctionBody = false

        ctx.functions[funcSymbol.index] = FunctionInfo(
            name = decl.name,
            startIp = startIp,
            paramCount = decl.params.size,
            localCount = localCount,
            returnsValue = true
        )
    }

    private fun compileProcedure(decl: Decl.Procedure) {
        val funcSymbol = ctx.symbols.resolve(decl.name) as dev.iris.compiler.symbols.Symbol.Function
        val startIp = ctx.emitter.currentAddress()
        ctx.currentFunctionReturnsValue = false
        ctx.inFunctionBody = true

        ctx.symbols.enterScope(isFunction = true)

        for (param in decl.params) {
            ctx.symbols.defineLocal(param.name, param.type)
        }

        for (i in (decl.params.size - 1) downTo 0) {
            ctx.emitter.emit(OpCode.STORE_LOCAL, i.toLong())
        }

        stmtCompiler.compileStatements(decl.body.stmts)

        val localCount = ctx.symbols.exitScope()
        ctx.inFunctionBody = false

        ctx.emitter.emit(OpCode.RET_VOID)

        ctx.functions[funcSymbol.index] = FunctionInfo(
            name = decl.name,
            startIp = startIp,
            paramCount = decl.params.size,
            localCount = localCount,
            returnsValue = false
        )
    }
}
