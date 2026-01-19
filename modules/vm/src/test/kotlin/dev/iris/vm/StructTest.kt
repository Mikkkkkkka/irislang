package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class StructTest {
    @Test
    fun `ALLOC_STRUCT creates struct on heap`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 2),     // 0 - field count
                Instr(OpCode.ALLOC_STRUCT, 0), // 1 - allocate struct of type 0
                Instr(OpCode.POP),             // 2 - discard the ref
                Instr(OpCode.HALT)             // 3
            )
        )

        val vm = VirtualMachine()
        vm.run(program)
        // Test passes if no error is thrown
    }

    @Test
    fun `STORE_FIELD and LOAD_FIELD work correctly`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 2),     // 0 - field count
                Instr(OpCode.ALLOC_STRUCT, 0), // 1 - allocate struct, pushes ref
                Instr(OpCode.DUP),             // 2 - duplicate struct ref
                Instr(OpCode.PUSH_I64, 42),    // 3 - push value 42
                Instr(OpCode.STORE_FIELD, 0),  // 4 - store to field 0
                // Stack now has: [struct_ref]
                Instr(OpCode.LOAD_FIELD, 0),   // 5 - load field 0
                Instr(OpCode.PRINT_I64),       // 6 - should print 42
                Instr(OpCode.HALT)             // 7
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `struct fields are initialized to zero`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 3),     // 0 - field count
                Instr(OpCode.ALLOC_STRUCT, 0), // 1 - allocate struct
                Instr(OpCode.LOAD_FIELD, 1),   // 2 - load field 1
                Instr(OpCode.PRINT_I64),       // 3 - should print 0
                Instr(OpCode.HALT)             // 4
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `multiple struct fields work independently`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 3),     // 0 - field count
                Instr(OpCode.ALLOC_STRUCT, 0), // 1 - allocate struct
                Instr(OpCode.DUP),             // 2 - dup for first store
                Instr(OpCode.DUP),             // 3 - dup for second store

                // Store field[0] = 10
                Instr(OpCode.PUSH_I64, 10),    // 4
                Instr(OpCode.STORE_FIELD, 0),  // 5

                // Store field[1] = 20
                Instr(OpCode.PUSH_I64, 20),    // 6
                Instr(OpCode.STORE_FIELD, 1),  // 7

                // Load field[0] and store to local[0]
                Instr(OpCode.DUP),             // 8
                Instr(OpCode.LOAD_FIELD, 0),   // 9
                Instr(OpCode.STORE_LOCAL, 0),  // 10

                // Load field[1] and store to local[1]
                Instr(OpCode.LOAD_FIELD, 1),   // 11
                Instr(OpCode.STORE_LOCAL, 1),  // 12

                // Load both locals and add
                Instr(OpCode.LOAD_LOCAL, 0),   // 13
                Instr(OpCode.LOAD_LOCAL, 1),   // 14
                Instr(OpCode.ADD),             // 15 - 10 + 20 = 30
                Instr(OpCode.PRINT_I64),       // 16 - should print 30
                Instr(OpCode.HALT)             // 17
            ),
            functions = listOf(
                FunctionInfo(
                    name = "main",
                    startIp = 0,
                    paramCount = 0,
                    localCount = 2,
                    returnsValue = false
                )
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("30"), output)
    }

    @Test
    fun `NEW instruction allocates struct`() {
        // NEW is an alias for ALLOC_STRUCT but might have different semantics
        // For now, we'll treat it as a synonym
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 2),     // 0 - field count
                Instr(OpCode.NEW, 0),          // 1 - allocate struct of type 0
                Instr(OpCode.DUP),             // 2 - dup ref
                Instr(OpCode.PUSH_I64, 99),    // 3 - value
                Instr(OpCode.STORE_FIELD, 0),  // 4 - store to field 0
                Instr(OpCode.LOAD_FIELD, 0),   // 5 - load field 0
                Instr(OpCode.PRINT_I64),       // 6 - should print 99
                Instr(OpCode.HALT)             // 7
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("99"), output)
    }
}
