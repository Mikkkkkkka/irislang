package dev.iris.core.bytecode

import kotlin.test.Test
import kotlin.test.assertEquals

class BytecodeTest {
    @Test
    fun `all opcodes have unique ordinals`() {
        val opcodes = OpCode.entries
        val ordinals = opcodes.map { it.ordinal }.toSet()
        assertEquals(opcodes.size, ordinals.size)
    }

    @Test
    fun `can create instructions with operands`() {
        val pushInstr = Instr(OpCode.PUSH_I64, 42)
        assertEquals(OpCode.PUSH_I64, pushInstr.op)
        assertEquals(42, pushInstr.operand)
    }

    @Test
    fun `can create instructions without operands`() {
        val addInstr = Instr(OpCode.ADD)
        assertEquals(OpCode.ADD, addInstr.op)
        assertEquals(null, addInstr.operand)
    }
}
