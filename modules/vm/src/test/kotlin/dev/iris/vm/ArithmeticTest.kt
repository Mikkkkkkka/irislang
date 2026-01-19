package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ArithmeticTest {
    @Test
    fun `ADD instruction`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.ADD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        val vm = VirtualMachine()
        vm.run(program) { output.add(it) }

        assertEquals(listOf("30"), output)
    }

    @Test
    fun `SUB instruction`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 50),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.SUB),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        val vm = VirtualMachine()
        vm.run(program) { output.add(it) }

        assertEquals(listOf("30"), output)
    }

    @Test
    fun `MUL instruction`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 6),
                Instr(OpCode.PUSH_I64, 7),
                Instr(OpCode.MUL),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        val vm = VirtualMachine()
        vm.run(program) { output.add(it) }

        assertEquals(listOf("42"), output)
    }

    @Test
    fun `DIV instruction`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 84),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.DIV),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        val vm = VirtualMachine()
        vm.run(program) { output.add(it) }

        assertEquals(listOf("42"), output)
    }

    @Test
    fun `MOD instruction`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 17),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.MOD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        val vm = VirtualMachine()
        vm.run(program) { output.add(it) }

        assertEquals(listOf("2"), output)
    }

    @Test
    fun `NEG instruction`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.NEG),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        val vm = VirtualMachine()
        vm.run(program) { output.add(it) }

        assertEquals(listOf("-42"), output)
    }
}
