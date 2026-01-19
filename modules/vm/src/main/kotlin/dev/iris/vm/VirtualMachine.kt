package dev.iris.vm

import dev.iris.core.bytecode.BytecodeProgram
import dev.iris.core.bytecode.Instr
import dev.iris.core.bytecode.OpCode

class VirtualMachine {
    private val stack = ArrayDeque<Long>()
    private var locals = LongArray(256)
    private var globals = LongArray(256)

    fun run(program: BytecodeProgram, stdout: (String) -> Unit = ::println): VmResult {
        val code = program.instructions
        var ip = 0

        while (ip in code.indices) {
            val ins = code[ip]
            when (ins.op) {
                OpCode.PUSH_I64 -> stack.addLast(ins.operand!!)
                OpCode.PUSH_TRUE -> stack.addLast(1L)
                OpCode.PUSH_FALSE -> stack.addLast(0L)
                OpCode.POP -> stack.removeLast()
                OpCode.DUP -> stack.addLast(stack.last())

                OpCode.ADD -> binaryOp { a, b -> a + b }
                OpCode.SUB -> binaryOp { a, b -> a - b }
                OpCode.MUL -> binaryOp { a, b -> a * b }
                OpCode.DIV -> binaryOp { a, b -> a / b }
                OpCode.MOD -> binaryOp { a, b -> a % b }
                OpCode.NEG -> stack.addLast(-stack.removeLast())

                OpCode.CMP_EQ -> binaryOp { a, b -> if (a == b) 1L else 0L }
                OpCode.CMP_NE -> binaryOp { a, b -> if (a != b) 1L else 0L }
                OpCode.CMP_LT -> binaryOp { a, b -> if (a < b) 1L else 0L }
                OpCode.CMP_LE -> binaryOp { a, b -> if (a <= b) 1L else 0L }
                OpCode.CMP_GT -> binaryOp { a, b -> if (a > b) 1L else 0L }
                OpCode.CMP_GE -> binaryOp { a, b -> if (a >= b) 1L else 0L }

                OpCode.AND -> binaryOp { a, b -> if (a != 0L && b != 0L) 1L else 0L }
                OpCode.OR -> binaryOp { a, b -> if (a != 0L || b != 0L) 1L else 0L }
                OpCode.NOT -> stack.addLast(if (stack.removeLast() == 0L) 1L else 0L)

                OpCode.LOAD_LOCAL -> stack.addLast(locals[ins.operand!!.toInt()])
                OpCode.STORE_LOCAL -> locals[ins.operand!!.toInt()] = stack.removeLast()
                OpCode.LOAD_GLOBAL -> stack.addLast(globals[ins.operand!!.toInt()])
                OpCode.STORE_GLOBAL -> globals[ins.operand!!.toInt()] = stack.removeLast()

                OpCode.JMP -> {
                    ip = ins.operand!!.toInt()
                    continue
                }
                OpCode.JMP_IF_FALSE -> {
                    if (stack.removeLast() == 0L) {
                        ip = ins.operand!!.toInt()
                        continue
                    }
                }
                OpCode.JMP_IF_TRUE -> {
                    if (stack.removeLast() != 0L) {
                        ip = ins.operand!!.toInt()
                        continue
                    }
                }

                OpCode.CALL -> error("CALL not implemented in basic VM")
                OpCode.RET -> return VmResult(exitCode = 0)
                OpCode.RET_VOID -> return VmResult(exitCode = 0)

                OpCode.ALLOC_ARR -> error("ALLOC_ARR not implemented in basic VM")
                OpCode.LOAD_ARR -> error("LOAD_ARR not implemented in basic VM")
                OpCode.STORE_ARR -> error("STORE_ARR not implemented in basic VM")
                OpCode.ALLOC_STRUCT -> error("ALLOC_STRUCT not implemented in basic VM")
                OpCode.LOAD_FIELD -> error("LOAD_FIELD not implemented in basic VM")
                OpCode.STORE_FIELD -> error("STORE_FIELD not implemented in basic VM")
                OpCode.NEW -> error("NEW not implemented in basic VM")

                OpCode.PRINT_I64 -> stdout(stack.removeLast().toString())
                OpCode.PRINT_BOOL -> stdout(if (stack.removeLast() != 0L) "правда" else "ложь")

                OpCode.HALT -> return VmResult(exitCode = 0)
            }
            ip++
        }
        return VmResult(exitCode = 1)
    }

    private inline fun binaryOp(op: (Long, Long) -> Long) {
        val b = stack.removeLast()
        val a = stack.removeLast()
        stack.addLast(op(a, b))
    }

    fun topOfStack(): Long? = stack.lastOrNull()
}
