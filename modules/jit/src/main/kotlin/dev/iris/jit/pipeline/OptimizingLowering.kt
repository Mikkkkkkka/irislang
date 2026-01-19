package dev.iris.jit.pipeline

import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.BytecodeProvider

class OptimizingLowering(
    private val provider: BytecodeProvider
) {
    fun lower(funcIndex: Int): LoweredFunction {
        val meta = provider.getFunctionMeta(funcIndex)
        val instrs = provider.getFunctionBytecode(funcIndex)
        val optimized = applyOptimizations(instrs)
        return LoweredFunction(funcIndex, meta, optimized)
    }

    private fun applyOptimizations(instructions: List<Instr>): List<Instr> {
        var current = instructions
        current = constantFolding(current)
        current = deadCodeElimination(current)
        return current
    }

    private fun constantFolding(instructions: List<Instr>): List<Instr> {
        val result = mutableListOf<Instr>()
        var i = 0

        while (i < instructions.size) {
            val instr = instructions[i]

            if (i + 2 < instructions.size &&
                instructions[i].op == OpCode.PUSH_I64 &&
                instructions[i + 1].op == OpCode.PUSH_I64
            ) {
                val a = instructions[i].operand ?: 0
                val b = instructions[i + 1].operand ?: 0
                val op = instructions[i + 2].op

                val folded = foldBinaryOp(a, b, op)
                if (folded != null) {
                    result.add(Instr(OpCode.PUSH_I64, folded))
                    i += 3
                    continue
                }
            }

            if (i + 1 < instructions.size &&
                instructions[i].op == OpCode.PUSH_I64 &&
                isUnaryOp(instructions[i + 1].op)
            ) {
                val value = instructions[i].operand ?: 0
                val op = instructions[i + 1].op

                val folded = foldUnaryOp(value, op)
                if (folded != null) {
                    result.add(Instr(OpCode.PUSH_I64, folded))
                    i += 2
                    continue
                }
            }

            result.add(instr)
            i++
        }

        return result
    }

    private fun foldBinaryOp(a: Long, b: Long, op: OpCode): Long? {
        return when (op) {
            OpCode.ADD -> a + b
            OpCode.SUB -> a - b
            OpCode.MUL -> a * b
            OpCode.DIV -> if (b != 0L) a / b else null
            OpCode.MOD -> if (b != 0L) a % b else null
            OpCode.CMP_EQ -> if (a == b) 1L else 0L
            OpCode.CMP_NE -> if (a != b) 1L else 0L
            OpCode.CMP_LT -> if (a < b) 1L else 0L
            OpCode.CMP_LE -> if (a <= b) 1L else 0L
            OpCode.CMP_GT -> if (a > b) 1L else 0L
            OpCode.CMP_GE -> if (a >= b) 1L else 0L
            OpCode.AND -> if (a != 0L && b != 0L) 1L else 0L
            OpCode.OR -> if (a != 0L || b != 0L) 1L else 0L
            else -> null
        }
    }

    private fun isUnaryOp(op: OpCode): Boolean {
        return op == OpCode.NEG || op == OpCode.NOT
    }

    private fun foldUnaryOp(value: Long, op: OpCode): Long? {
        return when (op) {
            OpCode.NEG -> -value
            OpCode.NOT -> if (value == 0L) 1L else 0L
            else -> null
        }
    }

    private fun deadCodeElimination(instructions: List<Instr>): List<Instr> {
        val result = mutableListOf<Instr>()
        var i = 0

        while (i < instructions.size) {
            val instr = instructions[i]

            if (i + 1 < instructions.size &&
                instructions[i].op == OpCode.PUSH_I64 &&
                instructions[i + 1].op == OpCode.POP
            ) {
                i += 2
                continue
            }

            if (i + 1 < instructions.size &&
                instructions[i].op == OpCode.PUSH_TRUE &&
                instructions[i + 1].op == OpCode.POP
            ) {
                i += 2
                continue
            }

            if (i + 1 < instructions.size &&
                instructions[i].op == OpCode.PUSH_FALSE &&
                instructions[i + 1].op == OpCode.POP
            ) {
                i += 2
                continue
            }

            if (i + 1 < instructions.size &&
                instructions[i].op == OpCode.DUP &&
                instructions[i + 1].op == OpCode.POP
            ) {
                i += 2
                continue
            }

            result.add(instr)
            i++
        }

        return result
    }
}
