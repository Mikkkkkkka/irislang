package dev.iris.jit.support

import dev.iris.jit.api.CompiledFunction
import dev.iris.jit.api.JitCompiler
import dev.iris.vm.VirtualMachine

/**
 * Simple mock JIT compiler for pipeline testing.
 * Produces a CompiledFunction that invokes the provided thunk.
 */
class MockJitCompiler(
    private val thunk: (funcIndex: Int) -> CompiledFunction
) : JitCompiler {
    override fun compile(funcIndex: Int): CompiledFunction = thunk(funcIndex)
}

class RunnableCompiledFunction(
    private val action: (VirtualMachine) -> Unit
) : CompiledFunction {
    override fun execute(vm: VirtualMachine) {
        action(vm)
    }
}

class SimpleCompiledFunction(
    private val action: () -> Unit
) : CompiledFunction {
    override fun execute(vm: VirtualMachine) {
        action()
    }
}
