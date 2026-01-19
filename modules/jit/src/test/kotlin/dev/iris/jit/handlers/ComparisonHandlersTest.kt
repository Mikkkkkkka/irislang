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

class ComparisonHandlersTest {

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
    fun `CMP_EQ returns 1 for equal values`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_EQ),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_EQ returns 0 for unequal values`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.CMP_EQ),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_EQ compares zero values`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.CMP_EQ),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_EQ compares negative values`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -10),
                Instr(OpCode.PUSH_I64, -10),
                Instr(OpCode.CMP_EQ),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_EQ with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.CMP_EQ),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("CMP_EQ"))
        }
    }

    @Test
    fun `CMP_NE returns 1 for unequal values`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.CMP_NE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_NE returns 0 for equal values`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_NE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_NE with zero and non-zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.CMP_NE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_NE with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.CMP_NE),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("CMP_NE"))
        }
    }

    @Test
    fun `CMP_LT returns 1 when a is less than b`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_LT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_LT returns 0 when a is greater than b`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_LT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_LT returns 0 when values are equal`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_LT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_LT with negative numbers`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -10),
                Instr(OpCode.PUSH_I64, -5),
                Instr(OpCode.CMP_LT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_LT with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.CMP_LT),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("CMP_LT"))
        }
    }

    @Test
    fun `CMP_LE returns 1 when a is less than b`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_LE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_LE returns 1 when values are equal`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_LE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_LE returns 0 when a is greater than b`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_LE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_LE with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.CMP_LE),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("CMP_LE"))
        }
    }

    @Test
    fun `CMP_GT returns 1 when a is greater than b`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_GT returns 0 when a is less than b`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_GT returns 0 when values are equal`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_GT with negative numbers`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -5),
                Instr(OpCode.PUSH_I64, -10),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_GT with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.CMP_GT),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("CMP_GT"))
        }
    }

    @Test
    fun `CMP_GE returns 1 when a is greater than b`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_GE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_GE returns 1 when values are equal`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_GE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `CMP_GE returns 0 when a is less than b`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_GE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `CMP_GE with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.CMP_GE),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("CMP_GE"))
        }
    }

    @Test
    fun `chained comparisons work correctly`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.CMP_LT),
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.CMP_GT),
                Instr(OpCode.CMP_EQ),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }
}
