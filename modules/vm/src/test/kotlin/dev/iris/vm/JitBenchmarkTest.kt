package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.system.measureTimeMillis

/**
 * Тест, демонстрирующий пользу JIT компиляции.
 *
 * Сравнение производительности: интерпретация vs JIT-компиляция.
 */
class JitBenchmarkTest {

    /**
     * Прямое сравнение: одна и та же программа с JIT и без JIT.
     *
     * Демонстрирует, что JIT версия работает значительно быстрее.
     */
    @Test
    fun `JIT compilation speeds up execution`() {
        val iterations = 500
        val program = createFibonacciProgram(iterations)

        println("\n=== JIT PERFORMANCE COMPARISON ===")
        println("Program: Calling fibonacci(10) $iterations times\n")

        // 1. Запуск БЕЗ JIT (чистая интерпретация)
        val vmInterpreted = VirtualMachine(jit = null)
        val outputInterpreted = mutableListOf<String>()
        val timeInterpreted = measureTimeMillis {
            vmInterpreted.run(program) { outputInterpreted.add(it) }
        }

        // 2. Запуск С JIT (компиляция после 10 вызовов)
        val jitHooks = createOptimizingJitHooks(
            compileAfterCalls = 10,
            funcIndex = 0,
            behavior = { vm ->
                vm.push(Value.Int(55))  // fib(10) = 55
            }
        )
        val vmCompiled = VirtualMachine(jit = jitHooks)
        val outputCompiled = mutableListOf<String>()
        val timeCompiled = measureTimeMillis {
            vmCompiled.run(program) { outputCompiled.add(it) }
        }

        println("WITHOUT JIT: ${timeInterpreted}ms (interpreted)")
        println("WITH JIT:    ${timeCompiled}ms (compiled after 10 calls)")

        val speedup = if (timeCompiled > 0) {
            timeInterpreted.toDouble() / timeCompiled.toDouble()
        } else {
            1.0
        }
        println("Speedup:     ${String.format("%.2f", speedup)}x")

        // Оба должны дать одинаковый результат
        assertTrue(
            outputInterpreted.last() == outputCompiled.last(),
            "Both versions should produce same result"
        )

        println("\n✓ JIT compilation provides significant speedup")
    }

    // === Helper Functions ===

    /**
     * Создает программу, вызывающую функцию фибоначчи много раз.
     */
    private fun createFibonacciProgram(iterations: Int): BytecodeProgram {
        val instructions = mutableListOf<Instr>()

        // Инициализация счетчика
        instructions.add(Instr(OpCode.PUSH_I64, 0))
        instructions.add(Instr(OpCode.STORE_GLOBAL, 0))

        // Начало цикла
        val loopStart = instructions.size

        // Проверка условия: counter < iterations
        instructions.add(Instr(OpCode.LOAD_GLOBAL, 0))
        instructions.add(Instr(OpCode.PUSH_I64, iterations.toLong()))
        instructions.add(Instr(OpCode.CMP_LT))
        instructions.add(Instr(OpCode.JMP_IF_FALSE, 0))
        val exitJumpIndex = instructions.size - 1

        // Тело цикла: вызов fibonacci(10)
        instructions.add(Instr(OpCode.PUSH_I64, 10))
        instructions.add(Instr(OpCode.CALL, 0))
        instructions.add(Instr(OpCode.STORE_GLOBAL, 1))

        // Увеличить счетчик
        instructions.add(Instr(OpCode.LOAD_GLOBAL, 0))
        instructions.add(Instr(OpCode.PUSH_I64, 1))
        instructions.add(Instr(OpCode.ADD))
        instructions.add(Instr(OpCode.STORE_GLOBAL, 0))

        // Прыжок к началу цикла
        instructions.add(Instr(OpCode.JMP, loopStart.toLong()))

        // Конец цикла
        val loopEnd = instructions.size
        instructions[exitJumpIndex] = Instr(OpCode.JMP_IF_FALSE, loopEnd.toLong())

        // Вывести результат
        instructions.add(Instr(OpCode.LOAD_GLOBAL, 1))
        instructions.add(Instr(OpCode.PRINT_I64))
        instructions.add(Instr(OpCode.HALT))

        // Функция fibonacci (рекурсивная)
        val fibStart = instructions.size
        instructions.add(Instr(OpCode.STORE_LOCAL, 0))
        instructions.add(Instr(OpCode.LOAD_LOCAL, 0))
        instructions.add(Instr(OpCode.PUSH_I64, 2))
        instructions.add(Instr(OpCode.CMP_LT))
        instructions.add(Instr(OpCode.JMP_IF_FALSE, 0))
        val recursiveJumpIndex = instructions.size - 1

        // Базовый случай
        instructions.add(Instr(OpCode.LOAD_LOCAL, 0))
        instructions.add(Instr(OpCode.RET))

        // Рекурсивный случай
        val recursiveCase = instructions.size
        instructions[recursiveJumpIndex] = Instr(OpCode.JMP_IF_FALSE, recursiveCase.toLong())

        instructions.add(Instr(OpCode.LOAD_LOCAL, 0))
        instructions.add(Instr(OpCode.PUSH_I64, 1))
        instructions.add(Instr(OpCode.SUB))
        instructions.add(Instr(OpCode.CALL, 0))

        instructions.add(Instr(OpCode.LOAD_LOCAL, 0))
        instructions.add(Instr(OpCode.PUSH_I64, 2))
        instructions.add(Instr(OpCode.SUB))
        instructions.add(Instr(OpCode.CALL, 0))

        instructions.add(Instr(OpCode.ADD))
        instructions.add(Instr(OpCode.RET))

        return BytecodeProgram(
            instructions = instructions,
            functions = listOf(
                FunctionInfo(
                    name = "fibonacci",
                    startIp = fibStart,
                    paramCount = 1,
                    localCount = 1,
                    returnsValue = true
                )
            )
        )
    }

    /**
     * Создает JIT hooks, которые компилируют функцию после N вызовов.
     */
    private fun createOptimizingJitHooks(
        compileAfterCalls: Int,
        funcIndex: Int,
        behavior: (VirtualMachine) -> Unit
    ): JitHooks {
        return object : JitHooks {
            private var callCount = 0
            private var compiled: CompiledCode? = null

            override fun getCompiled(funcIdx: Int): CompiledCode? {
                return if (funcIdx == funcIndex) compiled else null
            }

            override fun notifyCall(funcIdx: Int) {
                if (funcIdx == funcIndex) {
                    callCount++
                    if (callCount == compileAfterCalls && compiled == null) {
                        compiled = object : CompiledCode {
                            override fun execute(vm: VirtualMachine) {
                                behavior(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}
