package dev.iris.jit.support

import dev.iris.core.bytecode.Instr
import dev.iris.jit.api.BytecodeProvider
import dev.iris.jit.api.FunctionMeta

/**
 * Simple in-memory provider for tests and scaffolding.
 */
class InMemoryBytecodeProvider(
    private val functions: List<List<Instr>>,
    private val metas: List<FunctionMeta>,
    private val constPool: List<Long> = emptyList()
) : BytecodeProvider {
    override fun getFunctionBytecode(funcIndex: Int): List<Instr> = functions[funcIndex]
    override fun getConstPool(): List<Long> = constPool
    override fun getFunctionMeta(funcIndex: Int): FunctionMeta = metas[funcIndex]
}
