package dev.iris.jit.handlers

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.FunctionMeta
import dev.iris.jit.pipeline.BaselineCodeEmitter
import dev.iris.jit.pipeline.BytecodeLowering
import dev.iris.jit.pipeline.PipelineJitCompiler
import dev.iris.jit.runtime.StackUnderflowException
import dev.iris.jit.support.SingleFunctionProvider
import dev.iris.vm.VirtualMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ControlFlowHandlersTest {

    private fun compileWithOutput(instructions: List<Instr>, meta: FunctionMeta = FunctionMeta(0, 4, 16)): Pair<(VirtualMachine) -> Unit, MutableList<String>> {
        val output = mutableListOf<String>()
        val program = BytecodeProgram(instructions)
        val provider = SingleFunctionProvider(program, meta)
        val jit = PipelineJitCompiler(
            BytecodeLowering(provider),
            BaselineCodeEmitter(printSink = { output += it })
        )
        val compiled = jit.compile(0)
        return Pair({ vm -> compiled.execute(vm) }, output)
    }

    private fun compile(instructions: List<Instr>, meta: FunctionMeta = FunctionMeta(0, 4, 16)): (VirtualMachine) -> Unit {
        val program = BytecodeProgram(instructions)
        val provider = SingleFunctionProvider(program, meta)
        val jit = PipelineJitCompiler(
            BytecodeLowering(provider),
            BaselineCodeEmitter()
        )
        val compiled = jit.compile(0)
        return { vm -> compiled.execute(vm) }
    }

    @Test
    fun `JMP unconditionally jumps forward`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.JMP, 5),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "3"), output)
    }

    @Test
    fun `JMP can jump backward to create loop`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.ADD),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.CMP_LT),
                Instr(OpCode.JMP_IF_TRUE, 2),
                Instr(OpCode.HALT)
            ),
            FunctionMeta(0, 4, 16)
        )
        execute(VirtualMachine())
        assertEquals(listOf("0", "1", "2"), output)
    }

    @Test
    fun `JMP to immediate next instruction`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.JMP, 3),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "2"), output)
    }

    @Test
    fun `JMP_IF_FALSE jumps when condition is zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.JMP_IF_FALSE, 6),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "3"), output)
    }

    @Test
    fun `JMP_IF_FALSE does not jump when condition is non-zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.JMP_IF_FALSE, 6),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "2", "3"), output)
    }

    @Test
    fun `JMP_IF_FALSE treats negative value as true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, -1),
                Instr(OpCode.JMP_IF_FALSE, 6),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "2", "3"), output)
    }

    @Test
    fun `JMP_IF_FALSE with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.JMP_IF_FALSE, 2),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("JMP_IF_FALSE"))
        }
    }

    @Test
    fun `JMP_IF_FALSE consumes stack value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.JMP_IF_FALSE, 5),
                Instr(OpCode.POP),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("20", "10"), output)
    }

    @Test
    fun `JMP_IF_TRUE jumps when condition is non-zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.JMP_IF_TRUE, 6),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "3"), output)
    }

    @Test
    fun `JMP_IF_TRUE does not jump when condition is zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.JMP_IF_TRUE, 6),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "2", "3"), output)
    }

    @Test
    fun `JMP_IF_TRUE treats positive value as true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.JMP_IF_TRUE, 6),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "3"), output)
    }

    @Test
    fun `JMP_IF_TRUE treats negative value as true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, -100),
                Instr(OpCode.JMP_IF_TRUE, 6),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "3"), output)
    }

    @Test
    fun `JMP_IF_TRUE with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.JMP_IF_TRUE, 2),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("JMP_IF_TRUE"))
        }
    }

    @Test
    fun `JMP_IF_TRUE consumes stack value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.JMP_IF_TRUE, 5),
                Instr(OpCode.POP),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("20", "10"), output)
    }

    @Test
    fun `if-else pattern with JMP_IF_FALSE`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.JMP_IF_FALSE, 7),
                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.JMP, 9),
                Instr(OpCode.PUSH_I64, 200),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("100"), output)
    }

    @Test
    fun `if-else pattern takes else branch`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.JMP_IF_FALSE, 7),
                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.JMP, 9),
                Instr(OpCode.PUSH_I64, 200),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("200"), output)
    }

    @Test
    fun `while loop pattern`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.JMP_IF_FALSE, 13),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.SUB),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.JMP, 2),
                Instr(OpCode.HALT)
            ),
            FunctionMeta(0, 4, 16)
        )
        execute(VirtualMachine())
        assertEquals(listOf("3", "2", "1"), output)
    }

    @Test
    fun `nested conditional jumps`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.JMP_IF_FALSE, 9),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.JMP_IF_FALSE, 7),
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.JMP, 9),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `chained jumps`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.JMP, 2),
                Instr(OpCode.HALT),
                Instr(OpCode.JMP, 4),
                Instr(OpCode.HALT),
                Instr(OpCode.PUSH_I64, 999),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("999"), output)
    }

    @Test
    fun `JMP to HALT`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.JMP, 5),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }
}
