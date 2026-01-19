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

class StackHandlersTest {

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

    @Test
    fun `PUSH_I64 pushes positive value onto stack`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `PUSH_I64 pushes zero onto stack`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `PUSH_I64 pushes negative value onto stack`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -100),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-100"), output)
    }

    @Test
    fun `PUSH_I64 pushes large value onto stack`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, Long.MAX_VALUE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf(Long.MAX_VALUE.toString()), output)
    }

    @Test
    fun `PUSH_I64 pushes multiple values onto stack in order`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("3", "2", "1"), output)
    }

    @Test
    fun `PUSH_TRUE pushes 1 onto stack`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `PUSH_FALSE pushes 0 onto stack`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `PUSH_TRUE and PUSH_FALSE can be combined`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PUSH_FALSE),
                Instr(OpCode.PUSH_TRUE),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1", "0", "1"), output)
    }

    @Test
    fun `POP removes top value from stack`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.POP),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `POP on empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.POP),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("POP"))
        }
    }

    @Test
    fun `POP multiple values works correctly`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PUSH_I64, 3),
                Instr(OpCode.POP),
                Instr(OpCode.POP),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("1"), output)
    }

    @Test
    fun `DUP duplicates top value on stack`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.DUP),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42", "42"), output)
    }

    @Test
    fun `DUP on empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.DUP),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("DUP"))
        }
    }

    @Test
    fun `DUP preserves zero value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.DUP),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0", "0"), output)
    }

    @Test
    fun `DUP preserves negative value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -999),
                Instr(OpCode.DUP),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-999", "-999"), output)
    }

    @Test
    fun `DUP does not modify values below top`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.DUP),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("2", "2", "1"), output)
    }
}
