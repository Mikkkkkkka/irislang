package dev.iris.jit.runtime

import dev.iris.jit.api.CompiledFunction
import dev.iris.vm.CompiledCode
import dev.iris.vm.JitHooks
import dev.iris.vm.VirtualMachine

/**
 * Адаптер между AsyncJit и VM JitHooks интерфейсом.
 *
 * Связывает асинхронный JIT компилятор с виртуальной машиной,
 * позволяя VM использовать скомпилированные функции.
 */
class AsyncJitAdapter(
    private val asyncJit: AsyncJit
) : JitHooks {

    override fun getCompiled(funcIndex: Int): CompiledCode? {
        // Получаем скомпилированную функцию из AsyncJit
        val compiled = asyncJit.compiled(funcIndex) ?: return null

        // Адаптируем CompiledFunction → CompiledCode
        return object : CompiledCode {
            override fun execute(vm: VirtualMachine) {
                compiled.execute(vm)
            }
        }
    }

    override fun notifyCall(funcIndex: Int) {
        // Запускаем асинхронную компиляцию если еще не запущена
        asyncJit.ensureCompilation(funcIndex)
    }
}

/**
 * Extension функция для удобного создания адаптера.
 */
fun AsyncJit.asJitHooks(): JitHooks = AsyncJitAdapter(this)
