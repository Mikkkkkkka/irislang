package dev.iris.compiler.codegen

import dev.iris.compiler.CompilerContext
import dev.iris.compiler.InvalidOperationException
import dev.iris.compiler.symbols.Symbol
import dev.iris.core.ast.Expr
import dev.iris.core.ast.Operator
import dev.iris.core.bytecode.OpCode

class ExprCompiler(private val ctx: CompilerContext) {

    fun producesValue(expr: Expr): Boolean {
        return when (expr) {
            is Expr.Call -> {
                if (expr.callee == "печать") {
                    return false
                }
                val symbol = ctx.symbols.resolve(expr.callee)
                symbol is Symbol.Function && symbol.returnsValue
            }
            is Expr.Assign -> true
            else -> true
        }
    }

    fun compile(expr: Expr) {
        when (expr) {
            is Expr.IntLiteral -> compileIntLiteral(expr)
            is Expr.BoolLiteral -> compileBoolLiteral(expr)
            is Expr.NullLiteral -> compileNullLiteral()
            is Expr.Var -> compileVar(expr)
            is Expr.Binary -> compileBinary(expr)
            is Expr.Unary -> compileUnary(expr)
            is Expr.Call -> compileCall(expr)
            is Expr.FieldAccess -> compileFieldAccess(expr)
            is Expr.Deref -> compileDeref(expr)
            is Expr.Addr -> compileAddr(expr)
            is Expr.New -> compileNew(expr)
            is Expr.Assign -> compileAssign(expr)
            is Expr.ArrayAccess -> compileArrayAccess(expr)
            is Expr.ArrayLiteral -> compileArrayLiteral(expr)
        }
    }

    private fun compileIntLiteral(expr: Expr.IntLiteral) {
        ctx.emitter.emit(OpCode.PUSH_I64, expr.value.toLong())
    }

    private fun compileBoolLiteral(expr: Expr.BoolLiteral) {
        if (expr.value) {
            ctx.emitter.emit(OpCode.PUSH_TRUE)
        } else {
            ctx.emitter.emit(OpCode.PUSH_FALSE)
        }
    }

    private fun compileNullLiteral() {
        ctx.emitter.emit(OpCode.PUSH_I64, 0)
    }

    private fun compileVar(expr: Expr.Var) {
        val symbol = ctx.symbols.resolve(expr.name)
        when (symbol) {
            is Symbol.Variable -> {
                if (symbol.isGlobal) {
                    ctx.emitter.emit(OpCode.LOAD_GLOBAL, symbol.index.toLong())
                } else {
                    ctx.emitter.emit(OpCode.LOAD_LOCAL, symbol.index.toLong())
                }
            }
            is Symbol.Function -> throw InvalidOperationException("Cannot use function as value: ${expr.name}")
            is Symbol.Structure -> throw InvalidOperationException("Cannot use structure as value: ${expr.name}")
        }
    }

    private fun compileBinary(expr: Expr.Binary) {
        compile(expr.left)
        compile(expr.right)
        val opCode = when (expr.operator) {
            Operator.ADD -> OpCode.ADD
            Operator.SUBTRACT -> OpCode.SUB
            Operator.MULTIPLY -> OpCode.MUL
            Operator.DIVIDE -> OpCode.DIV
            Operator.REMAINING -> OpCode.MOD
            Operator.EQUAL -> OpCode.CMP_EQ
            Operator.NOT_EQUAL -> OpCode.CMP_NE
            Operator.LESS -> OpCode.CMP_LT
            Operator.LESS_EQUAL -> OpCode.CMP_LE
            Operator.GREATER -> OpCode.CMP_GT
            Operator.GREATER_EQUAL -> OpCode.CMP_GE
            Operator.AND -> OpCode.AND
            Operator.OR -> OpCode.OR
            else -> throw InvalidOperationException("Invalid binary operator: ${expr.operator}")
        }
        ctx.emitter.emit(opCode)
    }

    private fun compileUnary(expr: Expr.Unary) {
        compile(expr.expr)
        val opCode = when (expr.operator) {
            Operator.NEGATIVE -> OpCode.NEG
            Operator.NOT -> OpCode.NOT
            Operator.DEREFERENCE -> return
            Operator.ADDRESS -> return
            else -> throw InvalidOperationException("Invalid unary operator: ${expr.operator}")
        }
        ctx.emitter.emit(opCode)
    }

    private fun compileCall(expr: Expr.Call) {
        if (expr.callee == "печать") {
            if (expr.args.size != 1) {
                throw InvalidOperationException("печать expects 1 argument, got ${expr.args.size}")
            }
            compile(expr.args[0])
            ctx.emitter.emit(OpCode.PRINT_I64)
            return
        }

        val symbol = ctx.symbols.resolve(expr.callee)
        if (symbol !is Symbol.Function) {
            throw InvalidOperationException("Not a function: ${expr.callee}")
        }
        if (expr.args.size != symbol.paramCount) {
            throw InvalidOperationException(
                "Function ${expr.callee} expects ${symbol.paramCount} arguments, got ${expr.args.size}"
            )
        }
        for (arg in expr.args) {
            compile(arg)
        }
        ctx.emitter.emit(OpCode.CALL, symbol.index.toLong())
    }

    private fun compileFieldAccess(expr: Expr.FieldAccess) {
        compile(expr.target)
        val fieldIndex = resolveFieldIndex(expr.target, expr.field)
        ctx.emitter.emit(OpCode.LOAD_FIELD, fieldIndex.toLong())
    }

    private fun compileDeref(expr: Expr.Deref) {
        compile(expr.expr)
    }

    private fun compileAddr(expr: Expr.Addr) {
        compile(expr.expr)
    }

    private fun compileNew(expr: Expr.New) {
        val sizeExpr = expr.size
        if (sizeExpr != null) {
            compile(sizeExpr)
            ctx.emitter.emit(OpCode.ALLOC_ARR)
        } else {
            val structIndex = resolveStructIndex(expr.type)
            val struct = ctx.structures[structIndex]
            ctx.emitter.emit(OpCode.PUSH_I64, struct.fields.size.toLong())
            ctx.emitter.emit(OpCode.ALLOC_STRUCT, structIndex.toLong())
        }
    }

    private fun compileArrayAccess(expr: Expr.ArrayAccess) {
        compile(expr.array)
        compile(expr.index)
        ctx.emitter.emit(OpCode.LOAD_ARR)
    }

    private fun compileArrayLiteral(expr: Expr.ArrayLiteral) {
        val size = expr.elements.size
        ctx.emitter.emit(OpCode.PUSH_I64, size.toLong())
        ctx.emitter.emit(OpCode.ALLOC_ARR)
        for ((index, element) in expr.elements.withIndex()) {
            ctx.emitter.emit(OpCode.DUP)
            ctx.emitter.emit(OpCode.PUSH_I64, index.toLong())
            compile(element)
            ctx.emitter.emit(OpCode.STORE_ARR)
        }
    }

    private fun compileAssign(expr: Expr.Assign) {
        when (val target = expr.target) {
            is Expr.Var -> {
                compile(expr.expr)
                ctx.emitter.emit(OpCode.DUP)
                val symbol = ctx.symbols.resolve(target.name) as Symbol.Variable
                if (symbol.isGlobal) {
                    ctx.emitter.emit(OpCode.STORE_GLOBAL, symbol.index.toLong())
                } else {
                    ctx.emitter.emit(OpCode.STORE_LOCAL, symbol.index.toLong())
                }
            }
            is Expr.FieldAccess -> {
                compile(target.target)
                compile(expr.expr)
                val fieldIndex = resolveFieldIndex(target.target, target.field)
                ctx.emitter.emit(OpCode.STORE_FIELD, fieldIndex.toLong())
                compile(target.target)
                ctx.emitter.emit(OpCode.LOAD_FIELD, fieldIndex.toLong())
            }
            is Expr.ArrayAccess -> {
                compile(target.array)
                compile(target.index)
                compile(expr.expr)
                ctx.emitter.emit(OpCode.STORE_ARR)
                compile(expr.expr)
            }
            is Expr.Deref -> {
                compile(target.expr)
                compile(expr.expr)
                ctx.emitter.emit(OpCode.STORE_ARR)
                compile(target.expr)
                ctx.emitter.emit(OpCode.LOAD_ARR)
            }
            else -> throw InvalidOperationException("Invalid assignment target")
        }
    }

    private fun resolveFieldIndex(target: Expr, fieldName: String): Int {
        val structType = inferStructType(target)
        val struct = ctx.structures.find { it.name == structType }
            ?: throw InvalidOperationException("Unknown struct type: $structType")
        val fieldIndex = struct.fields.indexOfFirst { it.first == fieldName }
        if (fieldIndex < 0) {
            throw InvalidOperationException("Unknown field '$fieldName' in struct '$structType'")
        }
        return fieldIndex
    }

    private fun inferStructType(expr: Expr): String {
        return when (expr) {
            is Expr.Var -> {
                val symbol = ctx.symbols.resolve(expr.name)
                if (symbol is Symbol.Variable) {
                    when (val type = symbol.type) {
                        is dev.iris.core.ast.TypeRef.StructType -> type.name
                        is dev.iris.core.ast.TypeRef.Pointer -> {
                            val base = type.base
                            if (base is dev.iris.core.ast.TypeRef.StructType) base.name
                            else throw InvalidOperationException("Expected struct pointer type")
                        }
                        else -> throw InvalidOperationException("Expected struct type, got $type")
                    }
                } else {
                    throw InvalidOperationException("Expected variable for field access")
                }
            }
            is Expr.FieldAccess -> {
                val parentType = inferStructType(expr.target)
                val struct = ctx.structures.find { it.name == parentType }
                    ?: throw InvalidOperationException("Unknown struct type: $parentType")
                val field = struct.fields.find { it.first == expr.field }
                    ?: throw InvalidOperationException("Unknown field '${expr.field}' in struct '$parentType'")
                when (val fieldType = field.second) {
                    is dev.iris.core.ast.TypeRef.StructType -> fieldType.name
                    else -> throw InvalidOperationException("Expected struct type for nested field access")
                }
            }
            is Expr.Deref -> {
                inferStructType(expr.expr)
            }
            is Expr.New -> {
                when (val type = expr.type) {
                    is dev.iris.core.ast.TypeRef.StructType -> type.name
                    else -> throw InvalidOperationException("Expected struct type in new expression")
                }
            }
            else -> throw InvalidOperationException("Cannot determine struct type for field access")
        }
    }

    private fun resolveStructIndex(type: dev.iris.core.ast.TypeRef): Int {
        return when (type) {
            is dev.iris.core.ast.TypeRef.StructType -> {
                ctx.structures.indexOfFirst { it.name == type.name }.takeIf { it >= 0 }
                    ?: throw InvalidOperationException("Unknown struct: ${type.name}")
            }
            else -> throw InvalidOperationException("Not a struct type: $type")
        }
    }
}
