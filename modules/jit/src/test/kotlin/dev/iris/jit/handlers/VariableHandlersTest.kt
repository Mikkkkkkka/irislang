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

class VariableHandlersTest {

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
    fun `STORE_LOCAL and LOAD_LOCAL basic operation`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `STORE_LOCAL and LOAD_LOCAL with zero value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `STORE_LOCAL and LOAD_LOCAL with negative value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -999),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-999"), output)
    }

    @Test
    fun `STORE_LOCAL to different indices`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.STORE_LOCAL, 1),
                Instr(OpCode.PUSH_I64, 30),
                Instr(OpCode.STORE_LOCAL, 2),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.LOAD_LOCAL, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.LOAD_LOCAL, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("10", "20", "30"), output)
    }

    @Test
    fun `STORE_LOCAL overwrites previous value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 200),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("200"), output)
    }

    @Test
    fun `LOAD_LOCAL default value is zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `LOAD_LOCAL can be called multiple times`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42", "42"), output)
    }

    @Test
    fun `STORE_LOCAL with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("STORE_LOCAL"))
        }
    }

    @Test
    fun `STORE_GLOBAL and LOAD_GLOBAL basic operation`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `STORE_GLOBAL and LOAD_GLOBAL with zero value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `STORE_GLOBAL and LOAD_GLOBAL with negative value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, -500),
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("-500"), output)
    }

    @Test
    fun `STORE_GLOBAL to different indices`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.PUSH_I64, 200),
                Instr(OpCode.STORE_GLOBAL, 1),
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.LOAD_GLOBAL, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("100", "200"), output)
    }

    @Test
    fun `STORE_GLOBAL overwrites previous value`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 100),
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.PUSH_I64, 300),
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("300"), output)
    }

    @Test
    fun `LOAD_GLOBAL default value is zero`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("0"), output)
    }

    @Test
    fun `STORE_GLOBAL with empty stack throws StackUnderflowException`() {
        val execute = compile(
            listOf(
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.HALT)
            )
        )
        try {
            execute(VirtualMachine())
            fail("Expected StackUnderflowException")
        } catch (e: StackUnderflowException) {
            assertTrue(e.message!!.contains("STORE_GLOBAL"))
        }
    }

    @Test
    fun `locals and globals are independent`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("10", "20"), output)
    }

    @Test
    fun `local variable used in arithmetic`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 5),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.ADD),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("10"), output)
    }

    @Test
    fun `global variable used in arithmetic`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 7),
                Instr(OpCode.STORE_GLOBAL, 0),
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.LOAD_GLOBAL, 0),
                Instr(OpCode.MUL),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("49"), output)
    }

    @Test
    fun `increment local variable pattern`() {
        val (execute, output) = compileWithOutput(
            listOf(
                Instr(OpCode.PUSH_I64, 0),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.ADD),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.ADD),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        execute(VirtualMachine())
        assertEquals(listOf("2"), output)
    }
}
