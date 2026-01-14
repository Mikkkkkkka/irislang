package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ControlFlowTest {
    @Test
    fun `JMP instruction jumps unconditionally`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 1),     // 0
                Instr(OpCode.JMP, 4),          // 1 - jump to instruction 4
                Instr(OpCode.PUSH_I64, 99),    // 2 - should be skipped
                Instr(OpCode.PRINT_I64),       // 3 - should be skipped
                Instr(OpCode.PRINT_I64),       // 4 - jump target
                Instr(OpCode.HALT)             // 5
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `JMP_IF_FALSE jumps when condition is false`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 0),     // 0 - push false (0)
                Instr(OpCode.JMP_IF_FALSE, 4), // 1 - should jump to instruction 4
                Instr(OpCode.PUSH_I64, 99),    // 2 - should be skipped
                Instr(OpCode.PRINT_I64),       // 3 - should be skipped
                Instr(OpCode.PUSH_I64, 42),    // 4 - jump target
                Instr(OpCode.PRINT_I64),       // 5
                Instr(OpCode.HALT)             // 6
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `JMP_IF_FALSE does not jump when condition is true`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 1),     // 0 - push true (non-zero)
                Instr(OpCode.JMP_IF_FALSE, 5), // 1 - should not jump
                Instr(OpCode.PUSH_I64, 42),    // 2 - should execute
                Instr(OpCode.PRINT_I64),       // 3
                Instr(OpCode.HALT)             // 4
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `JMP_IF_TRUE jumps when condition is true`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 1),     // 0 - push true (non-zero)
                Instr(OpCode.JMP_IF_TRUE, 4),  // 1 - should jump to instruction 4
                Instr(OpCode.PUSH_I64, 99),    // 2 - should be skipped
                Instr(OpCode.PRINT_I64),       // 3 - should be skipped
                Instr(OpCode.PUSH_I64, 42),    // 4 - jump target
                Instr(OpCode.PRINT_I64),       // 5
                Instr(OpCode.HALT)             // 6
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `JMP_IF_TRUE does not jump when condition is false`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 0),     // 0 - push false (0)
                Instr(OpCode.JMP_IF_TRUE, 5),  // 1 - should not jump
                Instr(OpCode.PUSH_I64, 42),    // 2 - should execute
                Instr(OpCode.PRINT_I64),       // 3
                Instr(OpCode.HALT)             // 4
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `STORE_GLOBAL and LOAD_GLOBAL work correctly`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),    // 0 - push value
                Instr(OpCode.STORE_GLOBAL, 0), // 1 - store to global[0]
                Instr(OpCode.PUSH_I64, 99),    // 2 - push different value
                Instr(OpCode.LOAD_GLOBAL, 0),  // 3 - load from global[0]
                Instr(OpCode.PRINT_I64),       // 4 - should print 42
                Instr(OpCode.HALT)             // 5
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `multiple global variables work independently`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 10),    // 0
                Instr(OpCode.STORE_GLOBAL, 0), // 1 - store 10 to global[0]
                Instr(OpCode.PUSH_I64, 20),    // 2
                Instr(OpCode.STORE_GLOBAL, 1), // 3 - store 20 to global[1]
                Instr(OpCode.LOAD_GLOBAL, 0),  // 4 - load global[0]
                Instr(OpCode.LOAD_GLOBAL, 1),  // 5 - load global[1]
                Instr(OpCode.ADD),             // 6 - add them
                Instr(OpCode.PRINT_I64),       // 7 - should print 30
                Instr(OpCode.HALT)             // 8
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("30"), output)
    }
}
