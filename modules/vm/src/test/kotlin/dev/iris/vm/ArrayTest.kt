package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayTest {
    @Test
    fun `POP instruction discards top of stack`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 99),    // 0 - push value to discard
                Instr(OpCode.PUSH_I64, 42),    // 1 - push value to keep
                Instr(OpCode.POP),             // 2 - discard 42
                Instr(OpCode.PRINT_I64),       // 3 - should print 99
                Instr(OpCode.HALT)             // 4
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("99"), output)
    }

    @Test
    fun `DUP instruction duplicates top of stack`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),    // 0 - push value
                Instr(OpCode.DUP),             // 1 - duplicate it
                Instr(OpCode.PRINT_I64),       // 2 - print first copy
                Instr(OpCode.PRINT_I64),       // 3 - print second copy
                Instr(OpCode.HALT)             // 4
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42", "42"), output)
    }

    @Test
    fun `ALLOC_ARR creates array on heap`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 5),     // 0 - push array size
                Instr(OpCode.ALLOC_ARR),       // 1 - allocate array
                Instr(OpCode.POP),             // 2 - discard the ref
                Instr(OpCode.HALT)             // 3
            )
        )

        val vm = VirtualMachine()
        vm.run(program)
        // Test passes if no error is thrown
    }

    @Test
    fun `STORE_ARR and LOAD_ARR work correctly`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 3),     // 0 - array size
                Instr(OpCode.ALLOC_ARR),       // 1 - allocate array, pushes ref
                Instr(OpCode.DUP),             // 2 - duplicate array ref
                Instr(OpCode.PUSH_I64, 1),     // 3 - push index 1
                Instr(OpCode.PUSH_I64, 42),    // 4 - push value 42
                Instr(OpCode.STORE_ARR),       // 5 - store arr[1] = 42
                // Stack now has: [array_ref]
                Instr(OpCode.PUSH_I64, 1),     // 6 - push index 1
                Instr(OpCode.LOAD_ARR),        // 7 - load arr[1]
                Instr(OpCode.PRINT_I64),       // 8 - should print 42
                Instr(OpCode.HALT)             // 9
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `array elements are initialized to zero`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 5),     // 0 - array size
                Instr(OpCode.ALLOC_ARR),       // 1 - allocate array
                Instr(OpCode.PUSH_I64, 0),     // 2 - push index 0
                Instr(OpCode.LOAD_ARR),        // 3 - load arr[0]
                Instr(OpCode.PRINT_I64),       // 4 - should print 0
                Instr(OpCode.HALT)             // 5
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `multiple array operations work correctly`() {
        val program = BytecodeProgram(
            instructions = listOf(
                // Create array
                Instr(OpCode.PUSH_I64, 3),     // 0 - array size
                Instr(OpCode.ALLOC_ARR),       // 1 - allocate array, stack: [ref]

                // Store arr[0] = 10
                Instr(OpCode.DUP),             // 2 - stack: [ref, ref]
                Instr(OpCode.PUSH_I64, 0),     // 3 - stack: [ref, ref, 0]
                Instr(OpCode.PUSH_I64, 10),    // 4 - stack: [ref, ref, 0, 10]
                Instr(OpCode.STORE_ARR),       // 5 - stack: [ref]

                // Store arr[1] = 20
                Instr(OpCode.DUP),             // 6 - stack: [ref, ref]
                Instr(OpCode.PUSH_I64, 1),     // 7 - stack: [ref, ref, 1]
                Instr(OpCode.PUSH_I64, 20),    // 8 - stack: [ref, ref, 1, 20]
                Instr(OpCode.STORE_ARR),       // 9 - stack: [ref]

                // Load arr[0] and store to local[0]
                Instr(OpCode.DUP),             // 10 - stack: [ref, ref]
                Instr(OpCode.PUSH_I64, 0),     // 11 - stack: [ref, ref, 0]
                Instr(OpCode.LOAD_ARR),        // 12 - stack: [ref, 10]
                Instr(OpCode.STORE_LOCAL, 0),  // 13 - stack: [ref]

                // Load arr[1] and store to local[1]
                Instr(OpCode.PUSH_I64, 1),     // 14 - stack: [ref, 1]
                Instr(OpCode.LOAD_ARR),        // 15 - stack: [20]
                Instr(OpCode.STORE_LOCAL, 1),  // 16 - stack: []

                // Load both locals and add
                Instr(OpCode.LOAD_LOCAL, 0),   // 17 - stack: [10]
                Instr(OpCode.LOAD_LOCAL, 1),   // 18 - stack: [10, 20]
                Instr(OpCode.ADD),             // 19 - 10 + 20 = 30, stack: [30]
                Instr(OpCode.PRINT_I64),       // 20 - print 30
                Instr(OpCode.HALT)             // 21
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
}
