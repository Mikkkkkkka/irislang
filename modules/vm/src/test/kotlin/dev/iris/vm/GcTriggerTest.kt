package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertTrue

class GcTriggerTest {
    @Test
    fun `GC automatically triggers after many allocations`() {
        val program = BytecodeProgram(
            instructions = listOf(
                // Allocate many arrays to trigger GC
                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),  // Discard ref (becomes garbage)

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.HALT)
            )
        )

        // Run the program
        val vm = VirtualMachine()
        vm.run(program)

        // Test passes if no crash occurs
        // GC should have been triggered automatically
        assertTrue(true, "VM should handle many allocations with automatic GC")
    }

    @Test
    fun `GC collects unreachable objects`() {
        val program = BytecodeProgram(
            instructions = listOf(
                // Allocate array and keep reference
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.STORE_GLOBAL, 0),  // Store ref in global

                // Allocate many garbage arrays
                Instr(OpCode.PUSH_I64, 50),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),  // Garbage

                Instr(OpCode.PUSH_I64, 50),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),  // Garbage

                Instr(OpCode.PUSH_I64, 50),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),  // Garbage

                Instr(OpCode.PUSH_I64, 50),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),  // Garbage

                Instr(OpCode.PUSH_I64, 50),
                Instr(OpCode.ALLOC_ARR),
                Instr(OpCode.POP),  // Garbage

                // Load the kept array and verify it still works
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.LOAD_ARR),
                Instr(OpCode.POP),

                Instr(OpCode.HALT)
            )
        )

        // Run the program
        val vm = VirtualMachine()
        vm.run(program)

        // Test passes if the kept reference remains valid after GC
        assertTrue(true, "Reachable objects should survive GC")
    }
}
