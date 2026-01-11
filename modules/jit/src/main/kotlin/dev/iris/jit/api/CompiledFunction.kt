package dev.iris.jit.api

import dev.iris.vm.VirtualMachine

/**
 * Executable artifact produced by the JIT.
 */
interface CompiledFunction {
    fun execute(vm: VirtualMachine)
}
