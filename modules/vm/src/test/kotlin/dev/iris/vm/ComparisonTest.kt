package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ComparisonTest {
    @Test
    fun `CMP_EQ returns 1 for equal values`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.CMP_EQ),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_EQ returns 0 for unequal values`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PUSH_I64, 43),
                Instr(OpCode.CMP_EQ),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_LT works correctly`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.CMP_LT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_GT works correctly`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 30),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `AND logical operator`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `NOT logical operator`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.NOT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("1"), output)
    }
}
