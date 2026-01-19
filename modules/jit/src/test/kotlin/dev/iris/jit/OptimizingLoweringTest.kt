package dev.iris.jit

import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.FunctionMeta
import dev.iris.jit.pipeline.OptimizingLowering
import dev.iris.jit.support.InMemoryBytecodeProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class OptimizingLoweringTest {

    @Test
    fun `folds constant addition`() {
        val instrs = listOf(
            Instr(OpCode.PUSH_I64, 3),
            Instr(OpCode.PUSH_I64, 4),
            Instr(OpCode.ADD),
            Instr(OpCode.HALT)
        )
        val provider = createProvider(instrs)
        val lowering = OptimizingLowering(provider)

        val result = lowering.lower(0)

        assertEquals(2, result.instructions.size)
        assertEquals(OpCode.PUSH_I64, result.instructions[0].op)
        assertEquals(7L, result.instructions[0].operand)
        assertEquals(OpCode.HALT, result.instructions[1].op)
    }

    @Test
    fun `folds constant multiplication`() {
        val instrs = listOf(
            Instr(OpCode.PUSH_I64, 6),
            Instr(OpCode.PUSH_I64, 7),
            Instr(OpCode.MUL),
            Instr(OpCode.HALT)
        )
        val provider = createProvider(instrs)
        val lowering = OptimizingLowering(provider)

        val result = lowering.lower(0)

        assertEquals(2, result.instructions.size)
        assertEquals(42L, result.instructions[0].operand)
    }

    @Test
    fun `folds constant comparison`() {
        val instrs = listOf(
            Instr(OpCode.PUSH_I64, 5),
            Instr(OpCode.PUSH_I64, 3),
            Instr(OpCode.CMP_GT),
            Instr(OpCode.HALT)
        )
        val provider = createProvider(instrs)
        val lowering = OptimizingLowering(provider)

        val result = lowering.lower(0)

        assertEquals(2, result.instructions.size)
        assertEquals(1L, result.instructions[0].operand)
    }

    @Test
    fun `folds unary negation`() {
        val instrs = listOf(
            Instr(OpCode.PUSH_I64, 5),
            Instr(OpCode.NEG),
            Instr(OpCode.HALT)
        )
        val provider = createProvider(instrs)
        val lowering = OptimizingLowering(provider)

        val result = lowering.lower(0)

        assertEquals(2, result.instructions.size)
        assertEquals(-5L, result.instructions[0].operand)
    }

    @Test
    fun `eliminates push followed by pop`() {
        val instrs = listOf(
            Instr(OpCode.PUSH_I64, 42),
            Instr(OpCode.POP),
            Instr(OpCode.HALT)
        )
        val provider = createProvider(instrs)
        val lowering = OptimizingLowering(provider)

        val result = lowering.lower(0)

        assertEquals(1, result.instructions.size)
        assertEquals(OpCode.HALT, result.instructions[0].op)
    }

    @Test
    fun `preserves non-constant operations`() {
        val instrs = listOf(
            Instr(OpCode.LOAD_LOCAL, 0),
            Instr(OpCode.PUSH_I64, 1),
            Instr(OpCode.ADD),
            Instr(OpCode.HALT)
        )
        val provider = createProvider(instrs)
        val lowering = OptimizingLowering(provider)

        val result = lowering.lower(0)

        assertEquals(4, result.instructions.size)
    }

    @Test
    fun `chains multiple constant folding passes`() {
        val instrs = listOf(
            Instr(OpCode.PUSH_I64, 2),
            Instr(OpCode.PUSH_I64, 3),
            Instr(OpCode.ADD),
            Instr(OpCode.PUSH_I64, 4),
            Instr(OpCode.MUL),
            Instr(OpCode.HALT)
        )
        val provider = createProvider(instrs)
        val lowering = OptimizingLowering(provider)

        val result = lowering.lower(0)

        assertEquals(4, result.instructions.size)
        assertEquals(5L, result.instructions[0].operand)
    }

    private fun createProvider(instrs: List<Instr>): InMemoryBytecodeProvider {
        val meta = FunctionMeta(arity = 0, localsCount = 1, stackLimit = 8)
        return InMemoryBytecodeProvider(
            functions = listOf(instrs),
            metas = listOf(meta)
        )
    }
}
