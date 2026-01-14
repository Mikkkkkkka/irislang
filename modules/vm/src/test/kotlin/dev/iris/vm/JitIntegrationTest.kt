package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JitIntegrationTest {
    @Test
    fun `VM works without JIT`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        VirtualMachine().run(program) { output.add(it) }
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `VM tracks function calls for JIT compilation`() {
        val compiledExecutionLog = mutableListOf<Int>()
        val callLog = mutableListOf<Int>()

        // Mock JIT hooks
        val mockJitHooks = object : JitHooks {
            private var callCount = 0
            private var compiled: CompiledCode? = null

            override fun getCompiled(funcIndex: Int): CompiledCode? {
                return compiled
            }

            override fun notifyCall(funcIndex: Int) {
                callLog.add(funcIndex)
                callCount++
                // Compile after 3 calls
                if (callCount >= 3 && compiled == null) {
                    compiled = object : CompiledCode {
                        override fun execute(vm: VirtualMachine) {
                            compiledExecutionLog.add(funcIndex)
                            // Simulate the function behavior: push 100 and return
                            vm.push(Value.Int(100))
                        }
                    }
                }
            }
        }

        val vm = VirtualMachine(jit = mockJitHooks)

        val program = BytecodeProgram(
            instructions = listOf(
                // Call function 0 multiple times
                Instr(OpCode.CALL, 0),         // 0 - call 1 (interpreted)
                Instr(OpCode.POP),             // 1 - discard result
                Instr(OpCode.CALL, 0),         // 2 - call 2 (interpreted)
                Instr(OpCode.POP),             // 3 - discard result
                Instr(OpCode.CALL, 0),         // 4 - call 3 (interpreted, triggers compilation)
                Instr(OpCode.POP),             // 5 - discard result
                Instr(OpCode.HALT),            // 6

                // Function 0 body (returns 42)
                Instr(OpCode.PUSH_I64, 42),    // 7 - function start
                Instr(OpCode.RET),             // 8 - return 42
            ),
            functions = listOf(
                FunctionInfo(
                    name = "hotFunc",
                    startIp = 7,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = true
                )
            )
        )

        val output = mutableListOf<String>()
        vm.run(program) { output.add(it) }

        // Verify calls were tracked
        assertEquals(3, callLog.size, "Should have tracked 3 calls")

        // After 3 calls, function should be compiled
        // Call again to test compiled execution
        val program2 = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.CALL, 0),         // Should use compiled version
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT),

                // Function 0 body (not executed, compiled version used)
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.RET),
            ),
            functions = listOf(
                FunctionInfo(
                    name = "hotFunc",
                    startIp = 3,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = true
                )
            )
        )

        val output2 = mutableListOf<String>()
        VirtualMachine(jit = mockJitHooks).run(program2) { output2.add(it) }

        // Should execute compiled version which returns 100
        assertEquals(listOf("100"), output2)
        assertTrue(compiledExecutionLog.contains(0), "Compiled function should have been executed")
    }

    @Test
    fun `VM calls interpreted version when not compiled`() {
        // Mock JIT hooks that never compiles
        val mockJitHooks = object : JitHooks {
            override fun getCompiled(funcIndex: Int): CompiledCode? = null
            override fun notifyCall(funcIndex: Int) {}
        }

        val vm = VirtualMachine(jit = mockJitHooks)

        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.CALL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT),

                // Function 0 body
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.RET),
            ),
            functions = listOf(
                FunctionInfo(
                    name = "func",
                    startIp = 3,
                    paramCount = 0,
                    localCount = 0,
                    returnsValue = true
                )
            )
        )

        val output = mutableListOf<String>()
        vm.run(program) { output.add(it) }

        // Should use interpreted version since not compiled
        assertEquals(listOf("42"), output)
    }
}
