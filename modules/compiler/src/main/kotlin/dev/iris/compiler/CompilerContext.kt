package dev.iris.compiler

import dev.iris.compiler.emit.BytecodeEmitter
import dev.iris.compiler.emit.Label
import dev.iris.compiler.symbols.SymbolTable
import dev.iris.core.ast.TypeRef
import dev.iris.core.bytecode.FunctionInfo

class CompilerContext {
    val symbols = SymbolTable()
    val emitter = BytecodeEmitter()
    val functions = mutableListOf<FunctionInfo>()
    val structures = mutableListOf<StructureInfo>()

    val breakLabels = ArrayDeque<Label>()
    val continueLabels = ArrayDeque<Label>()

    var currentFunctionReturnsValue: Boolean = false
    var inFunctionBody: Boolean = false
}

data class StructureInfo(
    val name: String,
    val index: Int,
    val fields: List<Pair<String, TypeRef>>
)
