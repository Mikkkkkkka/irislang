package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EndToEndTest {
    @Test
    fun `complete program with functions arrays and control flow`() {
        // Program that computes factorial using recursion
        // factorial(n) = n == 0 ? 1 : n * factorial(n-1)
        val program = BytecodeProgram(
            instructions = listOf(
                // main: call factorial(5) and print result
                Instr(OpCode.PUSH_I64, 5),      // 0 - push argument
                Instr(OpCode.CALL, 0),          // 1 - call factorial
                Instr(OpCode.PRINT_I64),        // 2 - print result (should be 120)
                Instr(OpCode.HALT),             // 3

                // factorial(n): recursive function
                Instr(OpCode.STORE_LOCAL, 0),   // 4 - store n to local[0]
                Instr(OpCode.LOAD_LOCAL, 0),    // 5 - load n
                Instr(OpCode.PUSH_I64, 0),      // 6 - push 0
                Instr(OpCode.CMP_EQ),           // 7 - n == 0?
                Instr(OpCode.JMP_IF_FALSE, 11), // 8 - if false, skip to recursive case
                // Base case: return 1
                Instr(OpCode.PUSH_I64, 1),      // 9 - push 1
                Instr(OpCode.RET),              // 10 - return 1

                // Recursive case: return n * factorial(n-1)
                Instr(OpCode.LOAD_LOCAL, 0),    // 11 - load n
                Instr(OpCode.LOAD_LOCAL, 0),    // 12 - load n again
                Instr(OpCode.PUSH_I64, 1),      // 13 - push 1
                Instr(OpCode.SUB),              // 14 - n - 1
                Instr(OpCode.CALL, 0),          // 15 - factorial(n-1)
                Instr(OpCode.MUL),              // 16 - n * factorial(n-1)
                Instr(OpCode.RET),              // 17 - return result
            ),
            functions = listOf(
                FunctionInfo(
                    name = "factorial",
                    startIp = 4,
                    paramCount = 1,
                    localCount = 1,
                    returnsValue = true
                )
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("120"), output)
    }

    @Test
    fun `program with arrays and loops`() {
        // Program that creates an array, fills it with values, and sums them
        val program = BytecodeProgram(
            instructions = listOf(
                // Create array of size 5
                Instr(OpCode.PUSH_I64, 5),      // 0
                Instr(OpCode.ALLOC_ARR),        // 1 - alloc array
                Instr(OpCode.STORE_GLOBAL, 0),  // 2 - store array ref

                // Fill array: arr[0]=10, arr[1]=20, arr[2]=30, arr[3]=40, arr[4]=50
                Instr(OpCode.LOAD_GLOBAL, 0),   // 3
                Instr(OpCode.DUP),              // 4
                Instr(OpCode.PUSH_I64, 0),      // 5
                Instr(OpCode.PUSH_I64, 10),     // 6
                Instr(OpCode.STORE_ARR),        // 7

                Instr(OpCode.DUP),              // 8
                Instr(OpCode.PUSH_I64, 1),      // 9
                Instr(OpCode.PUSH_I64, 20),     // 10
                Instr(OpCode.STORE_ARR),        // 11

                Instr(OpCode.DUP),              // 12
                Instr(OpCode.PUSH_I64, 2),      // 13
                Instr(OpCode.PUSH_I64, 30),     // 14
                Instr(OpCode.STORE_ARR),        // 15

                Instr(OpCode.DUP),              // 16
                Instr(OpCode.PUSH_I64, 3),      // 17
                Instr(OpCode.PUSH_I64, 40),     // 18
                Instr(OpCode.STORE_ARR),        // 19

                Instr(OpCode.DUP),              // 20
                Instr(OpCode.PUSH_I64, 4),      // 21
                Instr(OpCode.PUSH_I64, 50),     // 22
                Instr(OpCode.STORE_ARR),        // 23

                Instr(OpCode.POP),              // 24 - discard array ref

                // Sum array elements
                Instr(OpCode.PUSH_I64, 0),      // 25 - sum = 0
                Instr(OpCode.STORE_GLOBAL, 1),  // 26

                Instr(OpCode.PUSH_I64, 0),      // 27 - i = 0
                Instr(OpCode.STORE_GLOBAL, 2),  // 28

                // Loop: while i < 5
                Instr(OpCode.LOAD_GLOBAL, 2),   // 29 - load i
                Instr(OpCode.PUSH_I64, 5),      // 30
                Instr(OpCode.CMP_LT),           // 31 - i < 5?
                Instr(OpCode.JMP_IF_FALSE, 44), // 32 - exit loop (jump to instr 44)

                // sum += arr[i]
                Instr(OpCode.LOAD_GLOBAL, 1),   // 33 - load sum
                Instr(OpCode.LOAD_GLOBAL, 0),   // 34 - load array
                Instr(OpCode.LOAD_GLOBAL, 2),   // 35 - load i
                Instr(OpCode.LOAD_ARR),         // 36 - arr[i]
                Instr(OpCode.ADD),              // 37 - sum + arr[i]
                Instr(OpCode.STORE_GLOBAL, 1),  // 38 - store sum

                // i++
                Instr(OpCode.LOAD_GLOBAL, 2),   // 39 - load i
                Instr(OpCode.PUSH_I64, 1),      // 40
                Instr(OpCode.ADD),              // 41 - i + 1
                Instr(OpCode.STORE_GLOBAL, 2),  // 42 - store i

                Instr(OpCode.JMP, 29),          // 43 - loop back

                // Print sum
                Instr(OpCode.LOAD_GLOBAL, 1),   // 44 - load sum
                Instr(OpCode.PRINT_I64),        // 45 - should print 150
                Instr(OpCode.HALT),             // 46
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("150"), output)
    }

    @Test
    fun `program with structs and fields`() {
        // Program that creates a Point struct and computes distance from origin
        val program = BytecodeProgram(
            instructions = listOf(
                // Create Point struct with fields x, y
                Instr(OpCode.PUSH_I64, 2),      // 0 - field count
                Instr(OpCode.ALLOC_STRUCT, 0),  // 1 - alloc Point
                Instr(OpCode.DUP),              // 2

                // Set x = 3
                Instr(OpCode.PUSH_I64, 3),      // 3
                Instr(OpCode.STORE_FIELD, 0),   // 4 - store to field 0 (x)

                // Set y = 4
                Instr(OpCode.DUP),              // 5
                Instr(OpCode.PUSH_I64, 4),      // 6
                Instr(OpCode.STORE_FIELD, 1),   // 7 - store to field 1 (y)

                // Load x and store to local[0]
                Instr(OpCode.DUP),              // 8
                Instr(OpCode.LOAD_FIELD, 0),    // 9 - load x
                Instr(OpCode.STORE_LOCAL, 0),   // 10 - store x to local[0]

                // Load y and store to local[1]
                Instr(OpCode.LOAD_FIELD, 1),    // 11 - load y
                Instr(OpCode.STORE_LOCAL, 1),   // 12 - store y to local[1]

                // Compute x*x + y*y
                Instr(OpCode.LOAD_LOCAL, 0),    // 13 - load x
                Instr(OpCode.LOAD_LOCAL, 0),    // 14 - load x
                Instr(OpCode.MUL),              // 15 - x*x

                Instr(OpCode.LOAD_LOCAL, 1),    // 16 - load y
                Instr(OpCode.LOAD_LOCAL, 1),    // 17 - load y
                Instr(OpCode.MUL),              // 18 - y*y

                Instr(OpCode.ADD),              // 19 - x*x + y*y
                Instr(OpCode.PRINT_I64),        // 20 - print 25
                Instr(OpCode.HALT),             // 21
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
        assertEquals(listOf("25"), output)
    }

    @Test
    fun `program with nested function calls`() {
        // Program with nested function calls: main -> add -> multiply
        val program = BytecodeProgram(
            instructions = listOf(
                // main: call add(2, 3) and print
                Instr(OpCode.PUSH_I64, 2),      // 0
                Instr(OpCode.PUSH_I64, 3),      // 1
                Instr(OpCode.CALL, 0),          // 2 - call add
                Instr(OpCode.PRINT_I64),        // 3
                Instr(OpCode.HALT),             // 4

                // add(a, b): returns multiply(a, 2) + b
                Instr(OpCode.STORE_LOCAL, 1),   // 5 - store b
                Instr(OpCode.STORE_LOCAL, 0),   // 6 - store a
                Instr(OpCode.LOAD_LOCAL, 0),    // 7 - load a
                Instr(OpCode.PUSH_I64, 2),      // 8 - push 2
                Instr(OpCode.CALL, 1),          // 9 - call multiply(a, 2)
                Instr(OpCode.LOAD_LOCAL, 1),    // 10 - load b
                Instr(OpCode.ADD),              // 11 - multiply result + b
                Instr(OpCode.RET),              // 12 - return

                // multiply(x, y): returns x * y
                Instr(OpCode.STORE_LOCAL, 1),   // 13 - store y
                Instr(OpCode.STORE_LOCAL, 0),   // 14 - store x
                Instr(OpCode.LOAD_LOCAL, 0),    // 15 - load x
                Instr(OpCode.LOAD_LOCAL, 1),    // 16 - load y
                Instr(OpCode.MUL),              // 17 - x * y
                Instr(OpCode.RET),              // 18 - return
            ),
            functions = listOf(
                FunctionInfo(
                    name = "add",
                    startIp = 5,
                    paramCount = 2,
                    localCount = 2,
                    returnsValue = true
                ),
                FunctionInfo(
                    name = "multiply",
                    startIp = 13,
                    paramCount = 2,
                    localCount = 2,
                    returnsValue = true
                )
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        // multiply(2, 2) + 3 = 4 + 3 = 7
        assertEquals(listOf("7"), output)
    }
}
