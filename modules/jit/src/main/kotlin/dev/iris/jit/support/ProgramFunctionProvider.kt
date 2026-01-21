package dev.iris.jit.support

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.Instr
import dev.iris.jit.api.BytecodeProvider
import dev.iris.jit.api.FunctionMeta

/**
 * Bytecode provider that extracts function bodies from BytecodeProgram.
 *
 * Unlike SingleFunctionProvider which returns all instructions,
 * this provider correctly extracts only the function body based on
 * FunctionInfo.startIp from the program.
 */
class ProgramFunctionProvider(
    private val program: BytecodeProgram
) : BytecodeProvider {

    override fun getFunctionBytecode(funcIndex: Int): List<Instr> {
        if (funcIndex >= program.functions.size) {
            error("Function index out of bounds: $funcIndex")
        }

        val funcInfo = program.functions[funcIndex]
        val startIp = funcInfo.startIp

        // Find end of function (next function start or end of program)
        val endIp = if (funcIndex + 1 < program.functions.size) {
            program.functions[funcIndex + 1].startIp
        } else {
            program.instructions.size
        }

        // Extract function body and rewrite jump targets to be relative to function start
        val slice = program.instructions.subList(startIp, endIp)
        return slice.map { instr ->
            val op = instr.op
            if (op == dev.iris.core.bytecode.OpCode.JMP ||
                op == dev.iris.core.bytecode.OpCode.JMP_IF_FALSE ||
                op == dev.iris.core.bytecode.OpCode.JMP_IF_TRUE
            ) {
                val target = instr.operand ?: -1
                val relative = target - startIp
                Instr(op, relative.toLong())
            } else {
                instr
            }
        }
    }

    override fun getConstPool(): List<Long> = emptyList()

    override fun getFunctionMeta(funcIndex: Int): FunctionMeta {
        if (funcIndex >= program.functions.size) {
            error("Function index out of bounds: $funcIndex")
        }

        val funcInfo = program.functions[funcIndex]
        return FunctionMeta(
            arity = funcInfo.paramCount,
            localsCount = funcInfo.localCount,
            stackLimit = 64,  // Conservative estimate
            startIp = funcInfo.startIp
        )
    }
}
