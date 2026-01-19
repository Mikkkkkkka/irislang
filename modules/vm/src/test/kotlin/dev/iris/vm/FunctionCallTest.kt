package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class FunctionCallTest {
    @Test
    fun `LOAD_LOCAL and STORE_LOCAL work correctly`() {
        // Setup: we'll manually create a call frame with locals
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),    // 0 - push value
                Instr(OpCode.STORE_LOCAL, 0),  // 1 - store to local[0]
                Instr(OpCode.PUSH_I64, 99),    // 2 - push different value
                Instr(OpCode.LOAD_LOCAL, 0),   // 3 - load from local[0]
                Instr(OpCode.PRINT_I64),       // 4 - should print 42
                Instr(OpCode.HALT)             // 5
            ),
            functions = listOf(
                FunctionInfo(
                    name = "main",
                    startIp = 0,
                    paramCount = 0,
                    localCount = 1,
                    returnsValue = false
                )
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `multiple local variables work independently`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 10),    // 0
                Instr(OpCode.STORE_LOCAL, 0),  // 1 - store 10 to local[0]
                Instr(OpCode.PUSH_I64, 20),    // 2
                Instr(OpCode.STORE_LOCAL, 1),  // 3 - store 20 to local[1]
                Instr(OpCode.LOAD_LOCAL, 0),   // 4 - load local[0]
                Instr(OpCode.LOAD_LOCAL, 1),   // 5 - load local[1]
                Instr(OpCode.ADD),             // 6 - add them
                Instr(OpCode.PRINT_I64),       // 7 - should print 30
                Instr(OpCode.HALT)             // 8
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
    fun `simple function call and return with value`() {
        val program = BytecodeProgram(
            instructions = listOf(
                // main function (0-4)
                Instr(OpCode.CALL, 0),         // 0 - call function 0 (add42)
                Instr(OpCode.PRINT_I64),       // 1 - print returned value
                Instr(OpCode.HALT),            // 2

                // add42 function (3-7)
                Instr(OpCode.PUSH_I64, 40),    // 3 - function start
                Instr(OpCode.PUSH_I64, 2),     // 4
                Instr(OpCode.ADD),             // 5
                Instr(OpCode.RET),             // 6 - return with value
            ),
            functions = listOf(
                FunctionInfo(
                    name = "add42",
                    startIp = 3,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = true
                )
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `function with parameters via stack`() {
        val program = BytecodeProgram(
            instructions = listOf(
                // main function (0-5)
                Instr(OpCode.PUSH_I64, 10),    // 0 - push first argument
                Instr(OpCode.PUSH_I64, 20),    // 1 - push second argument
                Instr(OpCode.CALL, 0),         // 2 - call function 0 (add)
                Instr(OpCode.PRINT_I64),       // 3 - print result
                Instr(OpCode.HALT),            // 4

                // add function (5-9)
                Instr(OpCode.STORE_LOCAL, 1),  // 5 - pop second arg to local[1]
                Instr(OpCode.STORE_LOCAL, 0),  // 6 - pop first arg to local[0]
                Instr(OpCode.LOAD_LOCAL, 0),   // 7 - load first arg
                Instr(OpCode.LOAD_LOCAL, 1),   // 8 - load second arg
                Instr(OpCode.ADD),             // 9 - add them
                Instr(OpCode.RET),             // 10 - return result
            ),
            functions = listOf(
                FunctionInfo(
                    name = "add",
                    startIp = 5,
                    paramCount = 2,
                    localCount = 2,
                    returnsValue = true
                )
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("30"), output)
    }

    @Test
    fun `RET_VOID returns without a value`() {
        val program = BytecodeProgram(
            instructions = listOf(
                // main function (0-3)
                Instr(OpCode.CALL, 0),         // 0 - call printHello
                Instr(OpCode.PUSH_I64, 99),    // 1 - push value after call
                Instr(OpCode.PRINT_I64),       // 2 - print it
                Instr(OpCode.HALT),            // 3

                // printHello function (4-6)
                Instr(OpCode.PUSH_I64, 42),    // 4 - function start
                Instr(OpCode.PRINT_I64),       // 5 - print 42
                Instr(OpCode.RET_VOID),        // 6 - return without value
            ),
            functions = listOf(
                FunctionInfo(
                    name = "printHello",
                    startIp = 4,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = false
                )
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42", "99"), output)
    }

    @Test
    fun `nested function calls work correctly`() {
        val program = BytecodeProgram(
            instructions = listOf(
                // main function (0-3)
                Instr(OpCode.CALL, 0),         // 0 - call outer
                Instr(OpCode.PRINT_I64),       // 1 - print result
                Instr(OpCode.HALT),            // 2

                // outer function (3-6)
                Instr(OpCode.CALL, 1),         // 3 - call inner
                Instr(OpCode.PUSH_I64, 10),    // 4 - add 10
                Instr(OpCode.ADD),             // 5
                Instr(OpCode.RET),             // 6 - return

                // inner function (7-9)
                Instr(OpCode.PUSH_I64, 32),    // 7 - push 32
                Instr(OpCode.RET),             // 8 - return 32
            ),
            functions = listOf(
                FunctionInfo(
                    name = "outer",
                    startIp = 3,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = true
                ),
                FunctionInfo(
                    name = "inner",
                    startIp = 7,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = true
                )
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }
}
