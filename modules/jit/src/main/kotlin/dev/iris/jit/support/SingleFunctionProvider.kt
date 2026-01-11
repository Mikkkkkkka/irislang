package dev.iris.jit.support

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.Instr
import dev.iris.jit.api.BytecodeProvider
import dev.iris.jit.api.FunctionMeta

/**
 * Bytecode provider for a single-function BytecodeProgram.
 */
class SingleFunctionProvider(
    private val program: BytecodeProgram,
    private val meta: FunctionMeta = FunctionMeta(
        arity = 0,
        localsCount = 0,
        stackLimit = program.instructions.size + 8
    )
) : BytecodeProvider {
    override fun getFunctionBytecode(funcIndex: Int): List<Instr> {
        check(funcIndex == 0) { "Only funcIndex 0 is supported" }
        return program.instructions
    }

    override fun getConstPool(): List<Long> = emptyList()

    override fun getFunctionMeta(funcIndex: Int): FunctionMeta {
        check(funcIndex == 0) { "Only funcIndex 0 is supported" }
        return meta
    }
}
