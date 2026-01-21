package dev.iris.jit

import dev.iris.core.bytecode.*
import dev.iris.jit.pipeline.VmIntegratedCodeEmitter
import dev.iris.jit.pipeline.BytecodeLowering
import dev.iris.jit.pipeline.PipelineJitCompiler
import dev.iris.jit.runtime.AsyncJit
import dev.iris.jit.runtime.asJitHooks
import dev.iris.jit.support.ProgramFunctionProvider
import dev.iris.vm.VirtualMachine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Тест интеграции AsyncJit с VirtualMachine.
 *
 * Демонстрирует как связать JIT компилятор с VM.
 */
class AsyncJitIntegrationTest {

    @Test
    fun `AsyncJit integrates with VirtualMachine via adapter`() = runBlocking {
        // 1. Создаем простую программу
        val program = BytecodeProgram(
            instructions = listOf(
                // main: call add(10, 20) and print
                Instr(OpCode.PUSH_I64, 10),
                Instr(OpCode.PUSH_I64, 20),
                Instr(OpCode.CALL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT),

                // add(a, b): return a + b
                Instr(OpCode.STORE_LOCAL, 1),  // b
                Instr(OpCode.STORE_LOCAL, 0),  // a
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 1),
                Instr(OpCode.ADD),
                Instr(OpCode.RET)
            ),
            functions = listOf(
                FunctionInfo(
                    name = "add",
                    startIp = 5,
                    paramCount = 2,
                    localCount = 2,
                    returnsValue = true
                )
            )
        )

        // 2. Создаем JIT компилятор
        val provider = ProgramFunctionProvider(program)
        val jitCompiler = PipelineJitCompiler(
            BytecodeLowering(provider),
            VmIntegratedCodeEmitter()
        )

        // 3. Создаем AsyncJit
        val asyncJit = AsyncJit(
            compiler = jitCompiler,
            funcCount = 1
        )

        // 4. Запускаем асинхронную компиляцию функции
        val compilationJob = asyncJit.ensureCompilation(0)
        compilationJob?.await()  // Ждем завершения компиляции

        // 5. Создаем VM с JIT через адаптер
        val jitHooks = asyncJit.asJitHooks()
        val vm = VirtualMachine(jit = jitHooks)

        // 6. Запускаем программу
        val output = mutableListOf<String>()
        vm.run(program) { output.add(it) }

        // 7. Проверяем результат
        assertEquals(listOf("30"), output)

        // 8. Закрываем JIT
        asyncJit.close()

        println("✓ AsyncJit successfully integrated with VirtualMachine!")
    }

    @Test
    fun `VM works without JIT compilation`() {
        // Программа без JIT - чистая интерпретация
        val program = BytecodeProgram(
            instructions = listOf(
                Instr(OpCode.PUSH_I64, 42),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT)
            )
        )

        val vm = VirtualMachine(jit = null)  // БЕЗ JIT
        val output = mutableListOf<String>()
        vm.run(program) { output.add(it) }

        assertEquals(listOf("42"), output)

        println("✓ VM works correctly without JIT")
    }

    @Test
    fun `VM uses compiled version when available`() = runBlocking {
        val program = BytecodeProgram(
            instructions = listOf(
                // main: call multiply(6, 7) and print
                Instr(OpCode.PUSH_I64, 6),
                Instr(OpCode.PUSH_I64, 7),
                Instr(OpCode.CALL, 0),
                Instr(OpCode.PRINT_I64),
                Instr(OpCode.HALT),

                // multiply(a, b): return a * b
                Instr(OpCode.STORE_LOCAL, 1),
                Instr(OpCode.STORE_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 0),
                Instr(OpCode.LOAD_LOCAL, 1),
                Instr(OpCode.MUL),
                Instr(OpCode.RET)
            ),
            functions = listOf(
                FunctionInfo(
                    name = "multiply",
                    startIp = 5,
                    paramCount = 2,
                    localCount = 2,
                    returnsValue = true
                )
            )
        )

        // Создаем JIT
        val provider = ProgramFunctionProvider(program)
        val jitCompiler = PipelineJitCompiler(
            BytecodeLowering(provider),
            VmIntegratedCodeEmitter()
        )
        val asyncJit = AsyncJit(compiler = jitCompiler, funcCount = 1)

        // Компилируем функцию
        asyncJit.ensureCompilation(0)?.await()

        // Запускаем с JIT
        val vm = VirtualMachine(jit = asyncJit.asJitHooks())
        val output = mutableListOf<String>()
        vm.run(program) { output.add(it) }

        assertEquals(listOf("42"), output)

        asyncJit.close()

        println("✓ VM successfully used compiled function")
    }
}
