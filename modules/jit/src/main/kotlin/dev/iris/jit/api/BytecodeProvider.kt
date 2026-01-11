package dev.iris.jit.api

import dev.iris.core.bytecode.Instr

/**
 * Read-only bytecode access for the JIT. VM/loader provides snapshots.
 */
interface BytecodeProvider {
    fun getFunctionBytecode(funcIndex: Int): List<Instr>
    fun getConstPool(): List<Long>
    fun getFunctionMeta(funcIndex: Int): FunctionMeta
}
