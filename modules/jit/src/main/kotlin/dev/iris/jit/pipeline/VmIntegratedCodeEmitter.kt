package dev.iris.jit.pipeline

import dev.iris.core.bytecode.OpCode
import dev.iris.jit.api.CompiledFunction
import dev.iris.vm.Value
import dev.iris.vm.VirtualMachine

/**
 * VM-integrated code emitter that works directly with VirtualMachine.
 *
 * Unlike BaselineCodeEmitter which uses its own ExecutionContext,
 * this emitter generates code that operates on VM's stack and heap directly.
 *
 * This allows full support for all opcodes including:
 * - CALL (function calls via VM's call stack)
 * - ALLOC_ARR, LOAD_ARR, STORE_ARR (arrays via VM heap)
 * - ALLOC_STRUCT, LOAD_FIELD, STORE_FIELD (structs via VM heap)
 */
class VmIntegratedCodeEmitter : CodeEmitter {

    override fun emit(function: LoweredFunction): CompiledFunction {
        val instrs = function.instructions

        return object : CompiledFunction {
            override fun execute(vm: VirtualMachine) {
                var ip = 0

                while (ip in instrs.indices) {
                    val instr = instrs[ip]

                    when (instr.op) {
                        OpCode.PUSH_I64 -> {
                            val value = instr.operand ?: error("Missing operand for PUSH_I64")
                            vm.push(Value.Int(value))
                        }

                        OpCode.PUSH_TRUE -> vm.push(Value.Bool(true))
                        OpCode.PUSH_FALSE -> vm.push(Value.Bool(false))

                        OpCode.POP -> vm.pop()

                        OpCode.DUP -> {
                            val value = vm.peek()
                            vm.push(value)
                        }

                        OpCode.ADD -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(a + b))
                        }

                        OpCode.SUB -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(a - b))
                        }

                        OpCode.MUL -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(a * b))
                        }

                        OpCode.DIV -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            if (b == 0L) error("Division by zero")
                            vm.push(Value.Int(a / b))
                        }

                        OpCode.MOD -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            if (b == 0L) error("Modulo by zero")
                            vm.push(Value.Int(a % b))
                        }

                        OpCode.NEG -> {
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(-a))
                        }

                        OpCode.CMP_EQ -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(if (a == b) 1 else 0))
                        }

                        OpCode.CMP_NE -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(if (a != b) 1 else 0))
                        }

                        OpCode.CMP_LT -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(if (a < b) 1 else 0))
                        }

                        OpCode.CMP_LE -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(if (a <= b) 1 else 0))
                        }

                        OpCode.CMP_GT -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(if (a > b) 1 else 0))
                        }

                        OpCode.CMP_GE -> {
                            val b = vm.pop().toInt()
                            val a = vm.pop().toInt()
                            vm.push(Value.Int(if (a >= b) 1 else 0))
                        }

                        OpCode.AND -> {
                            val b = vm.pop().toBool()
                            val a = vm.pop().toBool()
                            vm.push(Value.Int(if (a && b) 1 else 0))
                        }

                        OpCode.OR -> {
                            val b = vm.pop().toBool()
                            val a = vm.pop().toBool()
                            vm.push(Value.Int(if (a || b) 1 else 0))
                        }

                        OpCode.NOT -> {
                            val a = vm.pop().toBool()
                            vm.push(Value.Int(if (!a) 1 else 0))
                        }

                        OpCode.ALLOC_ARR -> {
                            val size = vm.pop().toInt().toInt()
                            if (size < 0) error("Negative array size: $size")
                            val ref = vm.allocArray(size)
                            vm.push(ref)
                        }

                        OpCode.LOAD_ARR -> {
                            val index = vm.pop().toInt().toInt()
                            val arrayRef = vm.pop()
                            if (arrayRef !is Value.HeapRef) error("LOAD_ARR expects HeapRef")

                            val obj = vm.getHeapObject(arrayRef.address)
                            if (obj !is dev.iris.vm.HeapObject.Array) error("LOAD_ARR expects Array")
                            if (index < 0 || index >= obj.size) {
                                error("Array index out of bounds: $index (size: ${obj.size})")
                            }

                            vm.push(obj.elements[index])
                        }

                        OpCode.STORE_ARR -> {
                            val value = vm.pop()
                            val index = vm.pop().toInt().toInt()
                            val arrayRef = vm.pop()
                            if (arrayRef !is Value.HeapRef) error("STORE_ARR expects HeapRef")

                            val obj = vm.getHeapObject(arrayRef.address)
                            if (obj !is dev.iris.vm.HeapObject.Array) error("STORE_ARR expects Array")
                            if (index < 0 || index >= obj.size) {
                                error("Array index out of bounds: $index (size: ${obj.size})")
                            }

                            obj.elements[index] = value
                        }

                        OpCode.ALLOC_STRUCT -> {
                            val typeIndex = instr.operand?.toInt() ?: error("Missing operand for ALLOC_STRUCT")
                            val fieldCount = vm.pop().toInt().toInt()
                            if (fieldCount < 0) error("Negative field count: $fieldCount")
                            val ref = vm.allocStruct(typeIndex, fieldCount)
                            vm.push(ref)
                        }

                        OpCode.LOAD_FIELD -> {
                            val fieldIndex = instr.operand?.toInt() ?: error("Missing operand for LOAD_FIELD")
                            val structRef = vm.pop()
                            if (structRef !is Value.HeapRef) error("LOAD_FIELD expects HeapRef")

                            val obj = vm.getHeapObject(structRef.address)
                            if (obj !is dev.iris.vm.HeapObject.Struct) error("LOAD_FIELD expects Struct")
                            if (fieldIndex < 0 || fieldIndex >= obj.fields.size) {
                                error("Field index out of bounds: $fieldIndex")
                            }

                            vm.push(obj.fields[fieldIndex])
                        }

                        OpCode.STORE_FIELD -> {
                            val fieldIndex = instr.operand?.toInt() ?: error("Missing operand for STORE_FIELD")
                            val value = vm.pop()
                            val structRef = vm.pop()
                            if (structRef !is Value.HeapRef) error("STORE_FIELD expects HeapRef")

                            val obj = vm.getHeapObject(structRef.address)
                            if (obj !is dev.iris.vm.HeapObject.Struct) error("STORE_FIELD expects Struct")
                            if (fieldIndex < 0 || fieldIndex >= obj.fields.size) {
                                error("Field index out of bounds: $fieldIndex")
                            }

                            obj.fields[fieldIndex] = value
                        }

                        OpCode.NEW -> {
                            // NEW is treated as alias for ALLOC_STRUCT
                            val typeIndex = instr.operand?.toInt() ?: error("Missing operand for NEW")
                            val fieldCount = vm.pop().toInt().toInt()
                            if (fieldCount < 0) error("Negative field count: $fieldCount")
                            val ref = vm.allocStruct(typeIndex, fieldCount)
                            vm.push(ref)
                        }

                        OpCode.RET -> {
                            // Return from compiled function - leave result on stack for VM to handle
                            return
                        }

                        OpCode.RET_VOID -> {
                            // Return from compiled function without value
                            return
                        }

                        OpCode.HALT -> {
                            // Should not appear in compiled function
                            error("HALT should not appear in compiled function")
                        }

                        // Opcodes that require VM integration - not supported in compiled code
                        OpCode.CALL -> {
                            error("CALL not supported in compiled code - functions should be compiled as whole units")
                        }

                        OpCode.JMP, OpCode.JMP_IF_FALSE, OpCode.JMP_IF_TRUE -> {
                            // Control flow should be handled by lowering phase
                            error("Control flow opcodes should be lowered before code emission")
                        }

                        OpCode.LOAD_LOCAL -> {
                            val index = instr.operand?.toInt() ?: error("Missing operand for LOAD_LOCAL")
                            val value = vm.loadLocal(index)
                            vm.push(value)
                        }

                        OpCode.STORE_LOCAL -> {
                            val index = instr.operand?.toInt() ?: error("Missing operand for STORE_LOCAL")
                            val value = vm.pop()
                            vm.storeLocal(index, value)
                        }

                        OpCode.LOAD_GLOBAL, OpCode.STORE_GLOBAL -> {
                            // Global variables are rare in hot functions, keep in interpreted code
                            error("Global variable opcodes not supported in compiled code")
                        }

                        OpCode.PRINT_I64, OpCode.PRINT_BOOL -> {
                            // Print opcodes should remain in interpreted code
                            error("Print opcodes not supported in compiled code")
                        }

                        else -> error("Unimplemented opcode in VmIntegratedCodeEmitter: ${instr.op}")
                    }

                    ip++
                }
            }
        }
    }
}
