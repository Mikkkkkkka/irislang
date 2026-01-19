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

class LogicalHandlersTest {

    private fun compileWithOutput(instructions: List<Instr>, meta: FunctionMeta = FunctionMeta(0, 0, 16)): Pair<(VirtualMachine) -> Unit, MutableList<String>> {
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

    private fun compile(instructions: List<Instr>, meta: FunctionMeta = FunctionMeta(0, 0, 16)): (VirtualMachine) -> Unit {
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
    fun `AND returns 1 when both operands are true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `AND returns 0 when first operand is false`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `AND returns 0 when second operand is false`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `AND returns 0 when both operands are false`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `AND treats non-zero as true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `AND treats negative as true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -1),
                Instr(OpCode.PUSH_I64, -100),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `AND with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.AND),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("AND"))
        }
    }

    @Test
    fun `AND with single element throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.AND),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("AND"))
        }
    }

    @Test
    fun `OR returns 1 when both operands are true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.OR),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `OR returns 1 when first operand is true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.OR),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `OR returns 1 when second operand is true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.OR),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `OR returns 0 when both operands are false`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.OR),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `OR treats non-zero as true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.OR),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `OR with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.OR),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("OR"))
        }
    }

    @Test
    fun `OR with single element throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.OR),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("OR"))
        }
    }

    @Test
    fun `NOT returns 1 for false`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.NOT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `NOT returns 0 for true`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.NOT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `NOT returns 1 for zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.NOT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `NOT returns 0 for non-zero positive`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.NOT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `NOT returns 0 for non-zero negative`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -100),
                Instr(OpCode.NOT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `NOT with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.NOT),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("NOT"))
        }
    }

    @Test
    fun `double NOT returns original truth value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.NOT),
                Instr(OpCode.NOT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `complex logical expression evaluates correctly`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.OR),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `NOT combined with AND`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.NOT),
                Instr(OpCode.AND),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `NOT combined with OR`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.NOT),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.OR),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }
}
