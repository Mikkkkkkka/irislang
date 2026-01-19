package dev.iris.jit.handlers

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.FunctionMeta
import dev.iris.jit.pipeline.BaselineCodeEmitter
import dev.iris.jit.pipeline.BytecodeLowering
import dev.iris.jit.pipeline.PipelineJitCompiler
import dev.iris.jit.runtime.DivisionByZeroException
import dev.iris.jit.runtime.StackUnderflowException
import dev.iris.jit.support.SingleFunctionProvider
import dev.iris.vm.VirtualMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ArithmeticHandlersTest {

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
    fun `ADD adds two positive numbers`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.ADD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("8"), output)
    }

    @Test
    fun `ADD adds positive and negative numbers`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, -3),
                Instr(OpCode.ADD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("7"), output)
    }

    @Test
    fun `ADD with zero returns same value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.ADD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `ADD with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.ADD),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("ADD"))
        }
    }

    @Test
    fun `ADD with single element throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.ADD),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("ADD"))
        }
    }

    @Test
    fun `SUB subtracts two positive numbers`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.SUB),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("7"), output)
    }

    @Test
    fun `SUB results in negative number`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.SUB),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-7"), output)
    }

    @Test
    fun `SUB subtracts negative number`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, -3),
                Instr(OpCode.SUB),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("8"), output)
    }

    @Test
    fun `SUB with zero returns same value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.SUB),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `SUB with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.SUB),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("SUB"))
        }
    }

    @Test
    fun `MUL multiplies two positive numbers`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 6),
                Instr(OpCode.PUSH_I64, 7),
                Instr(OpCode.MUL),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `MUL with zero returns zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 999),
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.MUL),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `MUL with negative number returns negative`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.PUSH_I64, -3),
                Instr(OpCode.MUL),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-15"), output)
    }

    @Test
    fun `MUL two negative numbers returns positive`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -4),
                Instr(OpCode.PUSH_I64, -5),
                Instr(OpCode.MUL),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("20"), output)
    }

    @Test
    fun `MUL with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.MUL),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("MUL"))
        }
    }

    @Test
    fun `DIV divides two positive numbers`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.PUSH_I64, 4),
                Instr(OpCode.DIV),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("5"), output)
    }

    @Test
    fun `DIV truncates towards zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 7),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.DIV),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("3"), output)
    }

    @Test
    fun `DIV with negative dividend`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -20),
                Instr(OpCode.PUSH_I64, 4),
                Instr(OpCode.DIV),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-5"), output)
    }

    @Test
    fun `DIV with negative divisor`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.PUSH_I64, -4),
                Instr(OpCode.DIV),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-5"), output)
    }

    @Test
    fun `DIV by zero throws DivisionByZeroException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.DIV),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected DivisionByZeroException")
        } catch (e: DivisionByZeroException) {
            assertTrue(e.message!!.contains("zero"))
        }
    }

    @Test
    fun `DIV with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.DIV),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("DIV"))
        }
    }

    @Test
    fun `MOD computes remainder`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 17),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.MOD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("2"), output)
    }

    @Test
    fun `MOD with exact division returns zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.MOD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `MOD with negative dividend`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -17),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.MOD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-2"), output)
    }

    @Test
    fun `MOD by zero throws DivisionByZeroException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.MOD),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected DivisionByZeroException")
        } catch (e: DivisionByZeroException) {
            assertTrue(e.message!!.contains("zero"))
        }
    }

    @Test
    fun `MOD with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.MOD),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("MOD"))
        }
    }

    @Test
    fun `NEG negates positive number`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.NEG),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-42"), output)
    }

    @Test
    fun `NEG negates negative number`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -42),
                Instr(OpCode.NEG),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `NEG of zero returns zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.NEG),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `NEG with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.NEG),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("NEG"))
        }
    }

    @Test
    fun `combined arithmetic operations work correctly`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.ADD),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.MUL),
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.SUB),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("21"), output)
    }
}
