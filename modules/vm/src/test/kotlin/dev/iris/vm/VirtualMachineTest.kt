package dev.iris.vm

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode
import kotlin.test.Test
import kotlin.test.assertEquals

class VirtualMachineTest {
    @Test
    fun printsValues() {
        val program = BytecodeProgram(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        val out = mutableListOf<String>()
        val vm = VirtualMachine()
        val res = vm.run(program) { out += it }
        assertEquals(0, res.exitCode)
        assertEquals(listOf("5"), out)
    }
}
