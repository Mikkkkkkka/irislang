package dev.iris.compiler.symbols

import dev.iris.compiler.DuplicateSymbolException
import dev.iris.compiler.UndefinedSymbolException
import dev.iris.core.ast.TypeRef

class SymbolTable {
    private var currentScope: Scope = Scope()
    private var nextGlobalIndex = 0
    private var nextFunctionIndex = 0
    private var nextStructIndex = 0

    fun enterScope(isFunction: Boolean = false) {
        currentScope = Scope(parent = currentScope, isFunction = isFunction)
    }

    fun exitScope(): Int {
        currentScope.propagateLocalCountToParent()
        val localCount = currentScope.localCount()
        currentScope = currentScope.parent ?: Scope()
        return localCount
    }

    fun define(symbol: Symbol): Symbol {
        if (currentScope.isDefined(symbol.name)) {
            throw DuplicateSymbolException(symbol.name)
        }
        return currentScope.define(symbol)
    }

    fun resolve(name: String): Symbol =
        currentScope.resolve(name) ?: throw UndefinedSymbolException(name)

    fun resolveOrNull(name: String): Symbol? =
        currentScope.resolve(name)

    fun isGlobalScope(): Boolean =
        currentScope.parent == null

    fun defineGlobal(name: String, type: TypeRef): Symbol.Variable {
        val symbol = Symbol.Variable(
            name = name,
            type = type,
            index = nextGlobalIndex++,
            isGlobal = true
        )
        return define(symbol) as Symbol.Variable
    }

    fun defineLocal(name: String, type: TypeRef): Symbol.Variable {
        val symbol = Symbol.Variable(
            name = name,
            type = type,
            index = currentScope.allocateLocal(),
            isGlobal = false
        )
        return define(symbol) as Symbol.Variable
    }

    fun defineFunction(name: String, returnType: TypeRef, paramCount: Int, returnsValue: Boolean): Symbol.Function {
        val symbol = Symbol.Function(
            name = name,
            type = returnType,
            index = nextFunctionIndex++,
            paramCount = paramCount,
            returnsValue = returnsValue
        )
        return define(symbol) as Symbol.Function
    }

    fun defineStruct(name: String, fields: List<Pair<String, TypeRef>>): Symbol.Structure {
        val symbol = Symbol.Structure(
            name = name,
            type = TypeRef.StructType(name),
            index = nextStructIndex++,
            fields = fields
        )
        return define(symbol) as Symbol.Structure
    }
}
