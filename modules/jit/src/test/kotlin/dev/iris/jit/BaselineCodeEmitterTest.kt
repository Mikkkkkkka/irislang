package dev.iris.jit

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.FunctionMeta
import dev.iris.jit.pipeline.BaselineCodeEmitter
import dev.iris.jit.pipeline.BytecodeLowering
import dev.iris.jit.pipeline.PipelineJitCompiler
import dev.iris.jit.support.SingleFunctionProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaselineCodeEmitterTest {

    @Test
    fun `baseline compiled function runs simple program`() {
        val printed = mutableListOf<String>()
        val program = BytecodeProgram(
            listOf(
                Instr(OpCode.PUSH_I64, 1),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.PUSH_I64, 2),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        val provider = SingleFunctionProvider(
            program,
            FunctionMeta(arity = 0, localsCount = 0, stackLimit = 8)
        )
        val jit = PipelineJitCompiler(
            BytecodeLowering(provider),
            BaselineCodeEmitter(printSink = { printed += it })
        )

        val compiled = jit.compile(0)
        compiled.execute(dev.iris.vm.VirtualMachine())

        assertEquals(listOf("1", "2"), printed)
    }

    @Test
    fun `throws on stack underflow`() {
        val program = BytecodeProgram(
            listOf(
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )
        val provider = SingleFunctionProvider(program)
        val jit = PipelineJitCompiler(
            BytecodeLowering(provider),
            BaselineCodeEmitter()
        )

        val compiled = jit.compile(0)
        var failed = false
        try {
            compiled.execute(dev.iris.vm.VirtualMachine())
        } catch (e: Exception) {
            failed = true
            assertTrue(e is dev.iris.jit.runtime.StackUnderflowException)
        }
        assertTrue(failed, "Expected StackUnderflowException")
    }
}
