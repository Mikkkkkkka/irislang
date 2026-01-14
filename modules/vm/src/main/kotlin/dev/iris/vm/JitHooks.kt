package dev.iris.vm

/**
 * Interface for JIT compilation hooks.
 * Allows VM to remain independent of JIT module while supporting JIT compilation.
 */
interface JitHooks {
    /**
     * Get compiled version of a function, if available.
     * Returns null if not yet compiled.
     */
    fun getCompiled(funcIndex: Int): CompiledCode?

    /**
     * Notify JIT that a function was called (for hot function detection).
     */
    fun notifyCall(funcIndex: Int)
}

/**
 * Compiled native code that can be executed by the VM.
 */
interface CompiledCode {
    /**
     * Execute the compiled code.
     * The implementation may manipulate the VM's stack and state.
     */
    fun execute(vm: VirtualMachine)
}
