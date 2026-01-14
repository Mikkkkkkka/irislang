package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.*

class VirtualMachineTest {
    @Test
    fun `can execute simple push and print`() {
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val output = mutableListOf<String>()
        val vm = VirtualMachine()
        val result = vm.run(program) { output.add(it) }

        assertEquals(0, result.exitCode)
        assertEquals(listOf("42"), output)
    }

    @Test
    fun `operand stack works correctly`() {
        val vm = VirtualMachine()

        vm.push(Value.Int(10))
        vm.push(Value.Int(20))

        assertEquals(Value.Int(20), vm.pop())
        assertEquals(Value.Int(10), vm.pop())
    }

    @Test
    fun `heap allocations work`() {
        val vm = VirtualMachine()
        val ref = vm.allocArray(5)

        assertIs<Value.HeapRef>(ref)
        val obj = vm.getHeapObject(ref.address)
        assertIs<HeapObject.Array>(obj)
    }
}
