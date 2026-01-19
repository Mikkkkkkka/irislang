package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PrintBoolTest {
    @Test
    fun `PRINT_BOOL prints true`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PRINT_BOOL),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("true"), output)
    }

    @Test
    fun `PRINT_BOOL prints false`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.PRINT_BOOL),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("false"), output)
    }

    @Test
    fun `PRINT_BOOL converts non-zero to true`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PRINT_BOOL),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("true"), output)
    }

    @Test
    fun `PRINT_BOOL converts zero to false`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.PRINT_BOOL),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("false"), output)
    }
}
