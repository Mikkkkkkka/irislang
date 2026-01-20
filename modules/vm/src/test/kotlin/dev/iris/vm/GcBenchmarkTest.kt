package dev.iris.vm

import dev.iris.core.bytecode.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тест, демонстрирующий пользу автоматической сборки мусора.
 *
 * Сравнение работы VM с разными настройками GC.
 */
class GcBenchmarkTest {

    /**
     * Демонстрация того, что GC освобождает память.
     *
     * Программа создает много объектов, которые становятся мусором.
     * С низким threshold GC запускается часто и освобождает память.
     * С высоким threshold GC не запускается и память накапливается.
     */
    @Test
    fun `GC collects garbage and frees memory`() {
        val allocations = 300

        println("\n=== GARBAGE COLLECTION DEMONSTRATION ===")
        println("Program: Creates $allocations arrays, most become garbage\n")

        // 1. БЕЗ частого GC (threshold очень высокий)
        val programNoGc = createMemoryIntensiveProgram(allocations)
        val vmNoGc = VirtualMachine(gcTriggerThreshold = 100000)
        val outputNoGc = mutableListOf<String>()
        vmNoGc.run(programNoGc) { outputNoGc.add(it) }
        val heapSizeNoGc = vmNoGc.heapObjectCount()

        // 2. С частым GC (threshold низкий)
        val programWithGc = createMemoryIntensiveProgram(allocations)
        val vmWithGc = VirtualMachine(gcTriggerThreshold = 50)
        val outputWithGc = mutableListOf<String>()
        vmWithGc.run(programWithGc) { outputWithGc.add(it) }
        val heapSizeWithGc = vmWithGc.heapObjectCount()

        println("WITHOUT frequent GC:")
        println("  Threshold: 100000 (very high)")
        println("  GC runs: ~0 times")
        println("  Heap objects: $heapSizeNoGc (accumulated)")

        println("\nWITH frequent GC:")
        println("  Threshold: 50 (low)")
        println("  GC runs: ~${allocations / 50} times")
        println("  Heap objects: $heapSizeWithGc (cleaned up)")

        val freed = heapSizeNoGc - heapSizeWithGc
        println("\n✓ GC freed $freed objects from heap!")
        println("✓ Memory reduction: ${heapSizeNoGc} → $heapSizeWithGc objects")

        // Оба должны дать одинаковый результат
        assertEquals(
            outputNoGc.last(),
            outputWithGc.last(),
            "Both should produce same result"
        )

        // С GC должно быть меньше объектов в памяти
        assertTrue(
            heapSizeWithGc < heapSizeNoGc,
            "GC should reduce heap size (was $heapSizeNoGc, now $heapSizeWithGc)"
        )
    }

    // === Helper Functions ===

    /**
     * Создает программу с интенсивным использованием памяти.
     *
     * Программа выделяет много массивов в цикле, большинство становятся мусором.
     * Только последний массив сохраняется и используется.
     */
    private fun createMemoryIntensiveProgram(allocations: Int): BytecodeProgram {
        val instructions = mutableListOf<Instr>()

        // Создать массив который будем хранить
        instructions.add(Instr(OpCode.PUSH_I64, 10))
        instructions.add(Instr(OpCode.ALLOC_ARR))
        instructions.add(Instr(OpCode.STORE_GLOBAL, 1))

        // Инициализация счетчика
        instructions.add(Instr(OpCode.PUSH_I64, 0))
        instructions.add(Instr(OpCode.STORE_GLOBAL, 0))

        // Начало цикла
        val loopStart = instructions.size

        // Условие: counter < allocations
        instructions.add(Instr(OpCode.LOAD_GLOBAL, 0))
        instructions.add(Instr(OpCode.PUSH_I64, allocations.toLong()))
        instructions.add(Instr(OpCode.CMP_LT))
        instructions.add(Instr(OpCode.JMP_IF_FALSE, 0))
        val exitJumpIndex = instructions.size - 1

        // Тело цикла: создать временный массив (GARBAGE)
        instructions.add(Instr(OpCode.PUSH_I64, 50))
        instructions.add(Instr(OpCode.ALLOC_ARR))
        instructions.add(Instr(OpCode.POP))  // Сразу выбросить - станет мусором

        // Инкремент счетчика
        instructions.add(Instr(OpCode.LOAD_GLOBAL, 0))
        instructions.add(Instr(OpCode.PUSH_I64, 1))
        instructions.add(Instr(OpCode.ADD))
        instructions.add(Instr(OpCode.STORE_GLOBAL, 0))

        // Прыжок к началу
        instructions.add(Instr(OpCode.JMP, loopStart.toLong()))

        // Конец цикла
        val loopEnd = instructions.size
        instructions[exitJumpIndex] = Instr(OpCode.JMP_IF_FALSE, loopEnd.toLong())

        // Использовать сохраненный массив (который пережил все GC)
        instructions.add(Instr(OpCode.LOAD_GLOBAL, 1))
        instructions.add(Instr(OpCode.PUSH_I64, 0))
        instructions.add(Instr(OpCode.PUSH_I64, 42))
        instructions.add(Instr(OpCode.STORE_ARR))

        instructions.add(Instr(OpCode.LOAD_GLOBAL, 1))
        instructions.add(Instr(OpCode.PUSH_I64, 0))
        instructions.add(Instr(OpCode.LOAD_ARR))
        instructions.add(Instr(OpCode.PRINT_I64))
        instructions.add(Instr(OpCode.HALT))

        return BytecodeProgram(instructions = instructions)
    }
}
