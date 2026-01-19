package dev.iris.compiler.emit

import dev.iris.compiler.UnresolvedLabelException
import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode

class BytecodeEmitter {
    private val instructions = mutableListOf<Instr>()

    fun emit(op: OpCode, operand: Long? = null): Int {
        val address = instructions.size
        instructions.add(Instr(op, operand))
        return address
    }

    fun currentAddress(): Int = instructions.size

    fun createLabel(): Label = Label()

    fun emitLabel(label: Label) {
        label.address = instructions.size
    }

    fun emitJump(op: OpCode, label: Label): Int {
        val address = instructions.size
        if (label.isResolved()) {
            instructions.add(Instr(op, label.address.toLong()))
        } else {
            label.pending.add(address)
            instructions.add(Instr(op, -1))
        }
        return address
    }

    fun patchJumps() {
        for (i in instructions.indices) {
            val instr = instructions[i]
            if (instr.operand == -1L && isJumpOpcode(instr.op)) {
                throw UnresolvedLabelException(i)
            }
        }
    }

    fun patchLabel(label: Label) {
        for (address in label.pending) {
            val instr = instructions[address]
            instructions[address] = Instr(instr.op, label.address.toLong())
        }
        label.pending.clear()
    }

    fun getInstructions(): List<Instr> = instructions.toList()

    private fun isJumpOpcode(op: OpCode): Boolean =
        op == OpCode.JMP || op == OpCode.JMP_IF_FALSE || op == OpCode.JMP_IF_TRUE
}
