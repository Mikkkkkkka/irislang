package dev.iris.compiler.codegen

import dev.iris.compiler.BreakOutsideLoopException
import dev.iris.compiler.CompilerContext
import dev.iris.compiler.ContinueOutsideLoopException
import dev.iris.core.ast.Stmt
import dev.iris.core.bytecode.OpCode

class StmtCompiler(private val ctx: CompilerContext) {
    private val exprCompiler = ExprCompiler(ctx)

    fun compile(stmt: Stmt) {
        when (stmt) {
            is Stmt.Block -> compileBlock(stmt)
            is Stmt.VarDecl -> compileVarDecl(stmt)
            is Stmt.If -> compileIf(stmt)
            is Stmt.While -> compileWhile(stmt)
            is Stmt.For -> compileFor(stmt)
            is Stmt.Return -> compileReturn(stmt)
            is Stmt.Break -> compileBreak()
            is Stmt.Continue -> compileContinue()
            is Stmt.ExprStmt -> compileExprStmt(stmt)
        }
    }

    fun compileStatements(stmts: List<Stmt>) {
        for (stmt in stmts) {
            compile(stmt)
        }
    }

    private fun compileBlock(stmt: Stmt.Block) {
        ctx.symbols.enterScope()
        for (s in stmt.stmts) {
            compile(s)
        }
        ctx.symbols.exitScope()
    }

    private fun compileVarDecl(stmt: Stmt.VarDecl) {
        val initExpr = stmt.init
        if (initExpr != null) {
            exprCompiler.compile(initExpr)
        } else {
            ctx.emitter.emit(OpCode.PUSH_I64, 0)
        }

        val symbol = if (ctx.inFunctionBody) {
            ctx.symbols.defineLocal(stmt.name, stmt.type)
        } else {
            ctx.symbols.defineGlobal(stmt.name, stmt.type)
        }

        if (symbol.isGlobal) {
            ctx.emitter.emit(OpCode.STORE_GLOBAL, symbol.index.toLong())
        } else {
            ctx.emitter.emit(OpCode.STORE_LOCAL, symbol.index.toLong())
        }
    }

    private fun compileIf(stmt: Stmt.If) {
        exprCompiler.compile(stmt.cond)

        val elseLabel = ctx.emitter.createLabel()
        val endLabel = ctx.emitter.createLabel()

        ctx.emitter.emitJump(OpCode.JMP_IF_FALSE, elseLabel)

        ctx.symbols.enterScope()
        for (s in stmt.thenBranch.stmts) {
            compile(s)
        }
        ctx.symbols.exitScope()

        val elseBranch = stmt.elseBranch
        if (elseBranch != null) {
            ctx.emitter.emitJump(OpCode.JMP, endLabel)
        }

        ctx.emitter.emitLabel(elseLabel)
        ctx.emitter.patchLabel(elseLabel)

        if (elseBranch != null) {
            compile(elseBranch)
            ctx.emitter.emitLabel(endLabel)
            ctx.emitter.patchLabel(endLabel)
        }
    }

    private fun compileWhile(stmt: Stmt.While) {
        val startLabel = ctx.emitter.createLabel()
        val endLabel = ctx.emitter.createLabel()

        ctx.breakLabels.addLast(endLabel)
        ctx.continueLabels.addLast(startLabel)

        ctx.emitter.emitLabel(startLabel)
        exprCompiler.compile(stmt.cond)
        ctx.emitter.emitJump(OpCode.JMP_IF_FALSE, endLabel)

        ctx.symbols.enterScope()
        for (s in stmt.body.stmts) {
            compile(s)
        }
        ctx.symbols.exitScope()

        ctx.emitter.emitJump(OpCode.JMP, startLabel)
        ctx.emitter.patchLabel(startLabel)

        ctx.emitter.emitLabel(endLabel)
        ctx.emitter.patchLabel(endLabel)

        ctx.breakLabels.removeLast()
        ctx.continueLabels.removeLast()
    }

    private fun compileFor(stmt: Stmt.For) {
        ctx.symbols.enterScope()

        val loopVar = ctx.symbols.defineLocal(stmt.varName, dev.iris.core.ast.TypeRef.IntType)
        exprCompiler.compile(stmt.start)
        ctx.emitter.emit(OpCode.STORE_LOCAL, loopVar.index.toLong())

        val startLabel = ctx.emitter.createLabel()
        val continueLabel = ctx.emitter.createLabel()
        val endLabel = ctx.emitter.createLabel()

        ctx.breakLabels.addLast(endLabel)
        ctx.continueLabels.addLast(continueLabel)

        ctx.emitter.emitLabel(startLabel)
        ctx.emitter.emit(OpCode.LOAD_LOCAL, loopVar.index.toLong())
        exprCompiler.compile(stmt.end)
        ctx.emitter.emit(OpCode.CMP_LT)
        ctx.emitter.emitJump(OpCode.JMP_IF_FALSE, endLabel)

        ctx.symbols.enterScope()
        for (s in stmt.body.stmts) {
            compile(s)
        }
        ctx.symbols.exitScope()

        ctx.emitter.emitLabel(continueLabel)
        ctx.emitter.patchLabel(continueLabel)

        ctx.emitter.emit(OpCode.LOAD_LOCAL, loopVar.index.toLong())
        ctx.emitter.emit(OpCode.PUSH_I64, 1)
        ctx.emitter.emit(OpCode.ADD)
        ctx.emitter.emit(OpCode.STORE_LOCAL, loopVar.index.toLong())

        ctx.emitter.emitJump(OpCode.JMP, startLabel)
        ctx.emitter.patchLabel(startLabel)

        ctx.emitter.emitLabel(endLabel)
        ctx.emitter.patchLabel(endLabel)

        ctx.breakLabels.removeLast()
        ctx.continueLabels.removeLast()

        ctx.symbols.exitScope()
    }

    private fun compileReturn(stmt: Stmt.Return) {
        val returnExpr = stmt.expr
        if (returnExpr != null) {
            exprCompiler.compile(returnExpr)
            ctx.emitter.emit(OpCode.RET)
        } else {
            ctx.emitter.emit(OpCode.RET_VOID)
        }
    }

    private fun compileBreak() {
        if (ctx.breakLabels.isEmpty()) {
            throw BreakOutsideLoopException()
        }
        val label = ctx.breakLabels.last()
        ctx.emitter.emitJump(OpCode.JMP, label)
    }

    private fun compileContinue() {
        if (ctx.continueLabels.isEmpty()) {
            throw ContinueOutsideLoopException()
        }
        val label = ctx.continueLabels.last()
        ctx.emitter.emitJump(OpCode.JMP, label)
    }

    private fun compileExprStmt(stmt: Stmt.ExprStmt) {
        exprCompiler.compile(stmt.expr)
        if (exprCompiler.producesValue(stmt.expr)) {
            ctx.emitter.emit(OpCode.POP)
        }
    }
}
