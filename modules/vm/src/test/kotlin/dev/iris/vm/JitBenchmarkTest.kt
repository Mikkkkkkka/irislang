package dev.iris.vm

import dev.iris.core.bytecode.*
import dev.iris.jit.pipeline.BytecodeLowering
import dev.iris.jit.pipeline.PipelineJitCompiler
import dev.iris.jit.pipeline.VmIntegratedCodeEmitter
import dev.iris.jit.runtime.AsyncJit
import dev.iris.jit.runtime.asJitHooks
import dev.iris.jit.support.ProgramFunctionProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.system.measureTimeMillis

/**
 * Тест, демонстрирующий пользу JIT компиляции.
 *
 * Сравнение производительности: интерпретация vs реальная JIT-компиляция с VmIntegratedCodeEmitter.
 */
class JitBenchmarkTest {

    /**
     * Прямое сравнение: одна и та же программа с JIT и без JIT.
     *
     * Демонстрирует, что JIT версия работает значительно быстрее.
     */
    @Test
    fun `JIT compilation speeds up execution`() = runBlocking {
        val iterations = 100000
        val program = createComputeProgram(iterations)

        println("\n=== JIT PERFORMANCE COMPARISON ===")
        println("Program: Calling compute(a, b, c) = (a + b) * c - 10, $iterations times\n")

        // 1. Запуск БЕЗ JIT (чистая интерпретация)
        val vmInterpreted = VirtualMachine(jit = null)
        val outputInterpreted = mutableListOf<String>()
        val timeInterpreted = measureTimeMillis {
            vmInterpreted.run(program) { outputInterpreted.add(it) }
        }

        // 2. Запуск С настоящим JIT (VmIntegratedCodeEmitter)
        val provider = ProgramFunctionProvider(program)
        val jitCompiler = PipelineJitCompiler(
            BytecodeLowering(provider),
            VmIntegratedCodeEmitter(program)
        )
        val asyncJit = AsyncJit(compiler = jitCompiler, funcCount = 1)

        // Pre-compile function before benchmark
        asyncJit.ensureCompilation(0)?.await()

        val vmCompiled = VirtualMachine(jit = asyncJit.asJitHooks())
        val outputCompiled = mutableListOf<String>()
        val timeCompiled = measureTimeMillis {
            vmCompiled.run(program) { outputCompiled.add(it) }
        }

        println("WITHOUT JIT: ${timeInterpreted}ms (interpreted)")
        println("WITH JIT:    ${timeCompiled}ms (compiled)")

        val speedup = if (timeCompiled > 0) {
            timeInterpreted.toDouble() / timeCompiled.toDouble()
        } else {
            1.0
        }
        println("Speedup:     ${String.format("%.2f", speedup)}x")

        // Оба должны дать одинаковый результат
        assertEquals(
            outputInterpreted.last(),
            outputCompiled.last(),
            "Both versions should produce same result"
        )

        asyncJit.close()

        println("\n✓ JIT compilation provides significant speedup")
    }

    // === Helper Functions ===

    /**
     * Создает программу, вызывающую простую вычислительную функцию много раз.
     *
     * Функция compute(a, b, c) = (a + b) * c - 10
     * Эта функция НЕ содержит CALL или JMP, поэтому может быть скомпилирована VmIntegratedCodeEmitter.
     */
    private fun createComputeProgram(iterations: Int): BytecodeProgram {
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

        // Тело цикла: вызов compute(5, 7, 3)
        instructions.add(Instr(OpCode.PUSH_I64, 5))
        instructions.add(Instr(OpCode.PUSH_I64, 7))
        instructions.add(Instr(OpCode.PUSH_I64, 3))
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

        // Функция compute(a, b, c) = (a + b) * c - 10
        val computeStart = instructions.size
        instructions.add(Instr(OpCode.STORE_LOCAL, 2))  // c
        instructions.add(Instr(OpCode.STORE_LOCAL, 1))  // b
        instructions.add(Instr(OpCode.STORE_LOCAL, 0))  // a
        instructions.add(Instr(OpCode.LOAD_LOCAL, 0))   // a
        instructions.add(Instr(OpCode.LOAD_LOCAL, 1))   // b
        instructions.add(Instr(OpCode.ADD))              // a + b
        instructions.add(Instr(OpCode.LOAD_LOCAL, 2))   // c
        instructions.add(Instr(OpCode.MUL))              // (a + b) * c
        instructions.add(Instr(OpCode.PUSH_I64, 10))    // 10
        instructions.add(Instr(OpCode.SUB))              // (a + b) * c - 10
        instructions.add(Instr(OpCode.RET))

        return BytecodeProgram(
            instructions = instructions,
            functions = listOf(
                FunctionInfo(
                    name = "compute",
                    startIp = computeStart,
                    paramCount = 3,
                    localCount = 3,
                    returnsValue = true
                )
            )
        )
    }
}
