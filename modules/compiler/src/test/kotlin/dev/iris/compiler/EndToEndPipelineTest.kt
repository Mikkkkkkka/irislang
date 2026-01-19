package dev.iris.compiler

import dev.iris.parser.lexer.Lexer
import dev.iris.parser.parser.Parser
import dev.iris.vm.VirtualMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EndToEndPipelineTest {

    private fun parseAndCompileAndRun(source: String, printer: (String) -> Unit = {}): VirtualMachine {
        val lexResult = Lexer(source).tokenize()
        assertTrue(lexResult.diagnostics.isEmpty(), "Lexer errors: ${lexResult.diagnostics}")

        val parseResult = Parser(lexResult.tokens).parse()
        assertTrue(
            parseResult.diagnostics.none { it.severity == dev.iris.core.Diagnostic.Severity.ERROR },
            "Parser errors: ${parseResult.diagnostics}"
        )
        assertNotNull(parseResult.program, "Parser returned null program")

        val compileResult = Compiler.compile(parseResult.program!!)
        assertNotNull(compileResult.program, "Compilation failed: ${compileResult.diagnostics}")
        assertTrue(compileResult.diagnostics.isEmpty(), "Compiler diagnostics: ${compileResult.diagnostics}")

        val vm = VirtualMachine()
        vm.run(compileResult.program, printer)
        return vm
    }

    private fun runAndCollectOutput(source: String): String {
        var output = ""
        val printer: (String) -> Unit = { message -> output += "$message\n" }
        parseAndCompileAndRun(source, printer)
        return output
    }

    private fun compileAndGetBytecode(source: String): dev.iris.core.bytecode.BytecodeProgram {
        val lexResult = Lexer(source).tokenize()
        assertTrue(lexResult.diagnostics.isEmpty(), "Lexer errors: ${lexResult.diagnostics}")

        val parseResult = Parser(lexResult.tokens).parse()
        assertTrue(
            parseResult.diagnostics.none { it.severity == dev.iris.core.Diagnostic.Severity.ERROR },
            "Parser errors: ${parseResult.diagnostics}"
        )
        assertNotNull(parseResult.program, "Parser returned null program")

        val compileResult = Compiler.compile(parseResult.program!!)
        assertNotNull(compileResult.program, "Compilation failed: ${compileResult.diagnostics}")
        assertTrue(compileResult.diagnostics.isEmpty(), "Compiler diagnostics: ${compileResult.diagnostics}")

        return compileResult.program
    }

    @Test
    fun `factorial of 5 returns 120`() {
        val source = """
            функция: целч факториал(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть 1;
                кц
                вернуть н * факториал(н - 1);
            кц

            целч результат = факториал(5);
            печать:результат;
        """.trimIndent()

        val expectedOutput = "120\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `factorial of 10 returns 3628800`() {
        val source = """
            функция: целч факториал(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть 1;
                кц
                вернуть н * факториал(н - 1);
            кц

            печать:(факториал(10));
        """.trimIndent()

        val expectedOutput = "3628800\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `factorial function compiles and runs correctly for small values`() {
        val source = """
            функция: целч факт(целч n)
            нч
                если (n <= 1)
                нч
                    вернуть 1;
                кц
                вернуть n * факт(n - 1);
            кц

            целч ф1 = факт(0);
            целч ф2 = факт(1);
            целч ф3 = факт(3);
            целч ф4 = факт(5);
            печать:ф1;
            печать:ф2;
            печать:ф3;
            печать:ф4;
        """.trimIndent()

        val expectedOutput = "1\n1\n6\n120\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `iterative factorial using while loop`() {
        val source = """
            функция: целч факториалИтер(целч н)
            нч
                целч результат = 1;
                целч к = 2;
                пока (к <= н)
                нч
                    результат = результат * к;
                    к = к + 1;
                кц
                вернуть результат;
            кц

            целч р5 = факториалИтер(5);
            целч р10 = факториалИтер(10);
            печать:р5;
            печать:р10;
        """.trimIndent()

        val expectedOutput = "120\n3628800\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `while loop summing numbers 1 to 10`() {
        val source = """
            целч сумма = 0;
            целч к = 1;
            пока (к <= 10)
            нч
                сумма = сумма + к;
                к = к + 1;
            кц
            печать:сумма;
        """.trimIndent()

        val expectedOutput = "55\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `nested while loops`() {
        val source = """
            целч итого = 0;
            целч к = 0;
            пока (к < 3)
            нч
                целч м = 0;
                пока (м < 4)
                нч
                    итого = итого + 1;
                    м = м + 1;
                кц
                к = к + 1;
            кц
            печать:итого;
        """.trimIndent()

        val expectedOutput = "12\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `while loop with break`() {
        val source = """
            целч счетчик = 0;
            целч к = 0;
            пока (к < 100)
            нч
                если (к >= 10)
                нч
                    выйти;
                кц
                счетчик = счетчик + 1;
                к = к + 1;
            кц
            печать:счетчик;
        """.trimIndent()

        val expectedOutput = "10\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `while loop with continue`() {
        val source = """
            целч сумма = 0;
            целч к = 0;
            пока (к < 10)
            нч
                к = к + 1;
                если (к % 2 == 0)
                нч
                    продолжить;
                кц
                сумма = сумма + к;
            кц
            печать:сумма;
        """.trimIndent()

        val expectedOutput = "25\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `MOD operation for prime check`() {
        val source = """
            функция: бул простое(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть ложь;
                кц
                целч к = 2;
                пока (к * к <= н)
                нч
                    если (н % к == 0)
                    нч
                        вернуть ложь;
                    кц
                    к = к + 1;
                кц
                вернуть правда;
            кц

            бул п2 = простое(2);
            бул п3 = простое(3);
            бул п4 = простое(4);
            бул п5 = простое(5);
            бул п7 = простое(7);
            бул п9 = простое(9);
            бул п11 = простое(11);
            печать:п2;
            печать:п3;
            печать:п4;
            печать:п5;
            печать:п7;
            печать:п9;
            печать:п11;
        """.trimIndent()

        val expectedOutput = "true\ntrue\nfalse\ntrue\ntrue\nfalse\ntrue\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `count primes up to N using MOD`() {
        val source = """
            функция: целч счетПростых(целч предел)
            нч
                целч счет = 0;
                целч н = 2;
                пока (н <= предел)
                нч
                    бул этоПростое = правда;
                    целч к = 2;
                    пока (к * к <= н)
                    нч
                        если (н % к == 0)
                        нч
                            этоПростое = ложь;
                            выйти;
                        кц
                        к = к + 1;
                    кц
                    если (этоПростое)
                    нч
                        счет = счет + 1;
                    кц
                    н = н + 1;
                кц
                вернуть счет;
            кц

            целч простыхДо10 = счетПростых(10);
            целч простыхДо20 = счетПростых(20);
            печать:простыхДо10;
            печать:простыхДо20;
        """.trimIndent()

        val expectedOutput = "4\n8\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `GCD using Euclidean algorithm with MOD`() {
        val source = """
            функция: целч нод(целч а, целч б)
            нч
                пока (б != 0)
                нч
                    целч времен = а % б;
                    а = б;
                    б = времен;
                кц
                вернуть а;
            кц

            целч р1 = нод(48, 18);
            целч р2 = нод(100, 35);
            целч р3 = нод(17, 13);
            печать:р1;
            печать:р2;
            печать:р3;
        """.trimIndent()

        val expectedOutput = "6\n5\n1\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `arithmetic operations`() {
        val source = """
            целч а = 10;
            целч б = 3;
            целч сумма = а + б;
            целч разн = а - б;
            целч произв = а * б;
            целч частн = а / б;
            целч остат = а % б;
            печать:сумма;
            печать:разн;
            печать:произв;
            печать:частн;
            печать:остат;
        """.trimIndent()

        val expectedOutput = "13\n7\n30\n3\n1\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `comparison operations`() {
        val source = """
            целч а = 5;
            целч б = 10;
            бул меньше = а < б;
            бул меньшеРавно = а <= б;
            бул больше = а > б;
            бул большеРавно = а >= б;
            бул равно = а == б;
            бул неРавно = а != б;
            печать:меньше;
            печать:меньшеРавно;
            печать:больше;
            печать:большеРавно;
            печать:равно;
            печать:неРавно;
        """.trimIndent()

        val expectedOutput = "true\ntrue\nfalse\nfalse\nfalse\ntrue\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `boolean operations`() {
        val source = """
            бул а = правда;
            бул б = ложь;
            бул резИ = а и б;
            бул резИли = а или б;
            бул неА = !а;
            бул неБ = !б;
            печать:резИ;
            печать:резИли;
            печать:неА;
            печать:неБ;
        """.trimIndent()

        val expectedOutput = "false\ntrue\nfalse\ntrue\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `if-else statements`() {
        val source = """
            целч х = 10;
            целч результат = 0;
            если (х > 5)
            нч
                результат = 1;
            кц
            иначе
            нч
                результат = 2;
            кц
            печать:результат;
        """.trimIndent()

        val expectedOutput = "1\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `nested if statements`() {
        val source = """
            целч х = 15;
            целч категория = 0;
            если (х < 10)
            нч
                категория = 1;
            кц
            иначе если (х < 20)
            нч
                категория = 2;
            кц
            иначе
            нч
                категория = 3;
            кц
            печать:категория;
        """.trimIndent()

        val expectedOutput = "2\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `function with multiple parameters`() {
        val source = """
            функция: целч макс(целч а, целч б)
            нч
                если (а > б)
                нч
                    вернуть а;
                кц
                вернуть б;
            кц

            функция: целч мин(целч а, целч б)
            нч
                если (а < б)
                нч
                    вернуть а;
                кц
                вернуть б;
            кц

            целч м1 = макс(10, 20);
            целч м2 = мин(10, 20);
            целч м3 = макс(мин(15, 25), макс(5, 10));
            печать:м1;
            печать:м2;
            печать:м3;
        """.trimIndent()

        val expectedOutput = "20\n10\n15\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `recursive fibonacci`() {
        val source = """
            функция: целч фиб(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть н;
                кц
                вернуть фиб(н - 1) + фиб(н - 2);
            кц

            целч ф5 = фиб(5);
            целч ф10 = фиб(10);
            печать:ф5;
            печать:ф10;
        """.trimIndent()

        val expectedOutput = "5\n55\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `power function iterative`() {
        val source = """
            функция: целч степень(целч основание, целч показатель)
            нч
                целч результат = 1;
                целч к = 0;
                пока (к < показатель)
                нч
                    результат = результат * основание;
                    к = к + 1;
                кц
                вернуть результат;
            кц

            целч р1 = степень(2, 10);
            целч р2 = степень(3, 5);
            целч р3 = степень(5, 3);
            печать:р1;
            печать:р2;
            печать:р3;
        """.trimIndent()

        val expectedOutput = "1024\n243\n125\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `procedure without return value`() {
        val source = """
            целч глобал = 0;

            процедура увеличить(целч значение)
            нч
                глобал = глобал + значение;
            кц

            увеличить(10);
            увеличить(20);
            увеличить(15);
            печать:глобал;
        """.trimIndent()

        val expectedOutput = "45\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `complex expression evaluation`() {
        val source = """
            целч а = 5;
            целч б = 3;
            целч в = 2;
            целч результат = (а + б) * в - (а - б) / в + а % б;
            печать:результат;
        """.trimIndent()

        val expectedOutput = "17\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `sum of even numbers using MOD`() {
        val source = """
            функция: целч суммаЧетных(целч н)
            нч
                целч сумма = 0;
                целч к = 0;
                пока (к <= н)
                нч
                    если (к % 2 == 0)
                    нч
                        сумма = сумма + к;
                    кц
                    к = к + 1;
                кц
                вернуть сумма;
            кц

            целч с10 = суммаЧетных(10);
            целч с20 = суммаЧетных(20);
            печать:с10;
            печать:с20;
        """.trimIndent()

        val expectedOutput = "30\n110\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `bubble sort simulation with swap count`() {
        val source = """
            функция: целч симуляцияСортировки(целч размер)
            нч
                целч перестановки = 0;
                целч к = 0;
                пока (к < размер - 1)
                нч
                    целч м = 0;
                    пока (м < размер - к - 1)
                    нч
                        перестановки = перестановки + 1;
                        м = м + 1;
                    кц
                    к = к + 1;
                кц
                вернуть перестановки;
            кц

            целч п5 = симуляцияСортировки(5);
            целч п10 = симуляцияСортировки(10);
            печать:п5;
            печать:п10;
        """.trimIndent()

        val expectedOutput = "10\n45\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `factorial comparison recursive vs iterative`() {
        val source = """
            функция: целч фактРекурс(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть 1;
                кц
                вернуть н * фактРекурс(н - 1);
            кц

            функция: целч фактИтер(целч н)
            нч
                целч рез = 1;
                целч к = 2;
                пока (к <= н)
                нч
                    рез = рез * к;
                    к = к + 1;
                кц
                вернуть рез;
            кц

            целч рекурс5 = фактРекурс(5);
            целч итер5 = фактИтер(5);
            целч рекурс10 = фактРекурс(10);
            целч итер10 = фактИтер(10);
            печать:рекурс5;
            печать:итер5;
            печать:рекурс10;
            печать:итер10;
        """.trimIndent()

        val expectedOutput = "120\n120\n3628800\n3628800\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `absolute value function`() {
        val source = """
            функция: целч абс(целч х)
            нч
                если (х < 0)
                нч
                    вернуть 0 - х;
                кц
                вернуть х;
            кц

            целч а1 = абс(5);
            целч а2 = абс(0);
            печать:а1;
            печать:а2;
        """.trimIndent()

        val expectedOutput = "5\n0\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `digit sum using MOD and DIV`() {
        val source = """
            функция: целч суммаЦифр(целч число)
            нч
                целч сумма = 0;
                пока (число > 0)
                нч
                    сумма = сумма + (число % 10);
                    число = число / 10;
                кц
                вернуть сумма;
            кц

            целч с123 = суммаЦифр(123);
            целч с9999 = суммаЦифр(9999);
            целч с12345 = суммаЦифр(12345);
            печать:с123;
            печать:с9999;
            печать:с12345;
        """.trimIndent()

        val expectedOutput = "6\n36\n15\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `count digits using while and DIV`() {
        val source = """
            функция: целч числоЦифр(целч число)
            нч
                если (число == 0)
                нч
                    вернуть 1;
                кц
                целч счетчик = 0;
                пока (число > 0)
                нч
                    счетчик = счетчик + 1;
                    число = число / 10;
                кц
                вернуть счетчик;
            кц

            целч ц1 = числоЦифр(0);
            целч ц2 = числоЦифр(5);
            целч ц3 = числоЦифр(123);
            целч ц4 = числоЦифр(99999);
            печать:ц1;
            печать:ц2;
            печать:ц3;
            печать:ц4;
        """.trimIndent()

        val expectedOutput = "1\n1\n3\n5\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `multiple functions calling each other`() {
        val source = """
            функция: целч удвоить(целч х)
            нч
                вернуть х * 2;
            кц

            функция: целч утроить(целч х)
            нч
                вернуть х * 3;
            кц

            функция: целч объединить(целч х)
            нч
                вернуть удвоить(х) + утроить(х);
            кц

            целч р = объединить(10);
            печать:р;
        """.trimIndent()

        val expectedOutput = "50\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `early return in function`() {
        val source = """
            функция: целч ранийВозврат(целч х)
            нч
                если (х < 0)
                нч
                    вернуть 0;
                кц
                если (х == 0)
                нч
                    вернуть 1;
                кц
                если (х < 10)
                нч
                    вернуть 2;
                кц
                вернуть 3;
            кц

            целч р1 = ранийВозврат(5);
            целч р2 = ранийВозврат(0);
            целч р3 = ранийВозврат(15);
            печать:р1;
            печать:р2;
            печать:р3;
        """.trimIndent()

        val expectedOutput = "2\n1\n3\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `benchmark factorial verifies correct bytecode generation`() {
        val source = """
            функция: целч факториал(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть 1;
                кц
                вернуть н * факториал(н - 1);
            кц

            целч результат5 = факториал(5);
            целч результат10 = факториал(10);
            печать:результат5;
            печать:результат10;
        """.trimIndent()

        val bytecode = compileAndGetBytecode(source)

        val factorialFunc = bytecode.functions.find { it.name == "факториал" }
        assertNotNull(factorialFunc, "factorial function should exist")
        assertEquals(1, factorialFunc.paramCount, "factorial should have 1 parameter")
        assertTrue(factorialFunc.returnsValue, "factorial should return a value")

        val mainFunc = bytecode.functions.find { it.name == "main" }
        assertNotNull(mainFunc, "main function should exist")

        val hasCallInstruction = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.CALL
        }
        assertTrue(hasCallInstruction, "bytecode should contain CALL instructions")

        val hasRetInstruction = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.RET
        }
        assertTrue(hasRetInstruction, "bytecode should contain RET instructions")

        val hasMulInstruction = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.MUL
        }
        assertTrue(hasMulInstruction, "bytecode should contain MUL instructions")

        val hasSubInstruction = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.SUB
        }
        assertTrue(hasSubInstruction, "bytecode should contain SUB instructions")

        val expectedOutput = "120\n3628800\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `benchmark array sum simulation verifies loop bytecode`() {
        val source = """
            функция: целч суммироватьДиапазон(целч начало, целч конец)
            нч
                целч сумма = 0;
                целч к = начало;
                пока (к <= конец)
                нч
                    сумма = сумма + к;
                    к = к + 1;
                кц
                вернуть сумма;
            кц

            целч сумма10 = суммироватьДиапазон(1, 10);
            целч сумма100 = суммироватьДиапазон(1, 100);
            печать:сумма10;
            печать:сумма100;
        """.trimIndent()

        val bytecode = compileAndGetBytecode(source)

        val sumFunc = bytecode.functions.find { it.name == "суммироватьДиапазон" }
        assertNotNull(sumFunc, "sum function should exist")
        assertEquals(2, sumFunc.paramCount, "sum function should have 2 parameters")
        assertTrue(sumFunc.returnsValue, "sum function should return a value")

        val hasJmpIfFalse = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.JMP_IF_FALSE
        }
        assertTrue(hasJmpIfFalse, "bytecode should contain JMP_IF_FALSE for while loop")

        val hasJmp = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.JMP
        }
        assertTrue(hasJmp, "bytecode should contain JMP for while loop")

        val hasAddInstruction = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.ADD
        }
        assertTrue(hasAddInstruction, "bytecode should contain ADD instructions")

        val hasCmpLe = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.CMP_LE
        }
        assertTrue(hasCmpLe, "bytecode should contain CMP_LE for loop condition")

        val expectedOutput = "55\n5050\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `benchmark MOD operations verifies modulo bytecode`() {
        val source = """
            функция: бул делитсяНа(целч число, целч делитель)
            нч
                вернуть число % делитель == 0;
            кц

            функция: целч считатьДелящиесяНа3(целч предел)
            нч
                целч счет = 0;
                целч к = 1;
                пока (к <= предел)
                нч
                    если (делитсяНа(к, 3))
                    нч
                        счет = счет + 1;
                    кц
                    к = к + 1;
                кц
                вернуть счет;
            кц

            целч результат = считатьДелящиесяНа3(30);
            печать:результат;
        """.trimIndent()

        val bytecode = compileAndGetBytecode(source)

        val divisibleFunc = bytecode.functions.find { it.name == "делитсяНа" }
        assertNotNull(divisibleFunc, "divisible function should exist")
        assertEquals(2, divisibleFunc.paramCount, "divisible function should have 2 parameters")

        val countFunc = bytecode.functions.find { it.name == "считатьДелящиесяНа3" }
        assertNotNull(countFunc, "count function should exist")
        assertEquals(1, countFunc.paramCount, "count function should have 1 parameter")

        val hasModInstruction = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.MOD
        }
        assertTrue(hasModInstruction, "bytecode should contain MOD instructions")

        val hasCmpEq = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.CMP_EQ
        }
        assertTrue(hasCmpEq, "bytecode should contain CMP_EQ instructions")

        val expectedOutput = "10\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `benchmark while loop verifies control flow bytecode`() {
        val source = """
            функция: целч суммаКвадратов(целч н)
            нч
                целч сумма = 0;
                целч к = 1;
                пока (к <= н)
                нч
                    сумма = сумма + к * к;
                    к = к + 1;
                кц
                вернуть сумма;
            кц

            целч квадраты5 = суммаКвадратов(5);
            целч квадраты10 = суммаКвадратов(10);
            печать:квадраты5;
            печать:квадраты10;
        """.trimIndent()

        val bytecode = compileAndGetBytecode(source)

        val sumSquaresFunc = bytecode.functions.find { it.name == "суммаКвадратов" }
        assertNotNull(sumSquaresFunc, "sumSquares function should exist")
        assertEquals(1, sumSquaresFunc.paramCount, "sumSquares function should have 1 parameter")
        assertTrue(sumSquaresFunc.localCount >= 3, "sumSquares function should have at least 3 locals")

        val jmpIfFalseCount = bytecode.instructions.count {
            it.op == dev.iris.core.bytecode.OpCode.JMP_IF_FALSE
        }
        assertTrue(jmpIfFalseCount >= 1, "bytecode should contain at least 1 JMP_IF_FALSE")

        val jmpCount = bytecode.instructions.count {
            it.op == dev.iris.core.bytecode.OpCode.JMP
        }
        assertTrue(jmpCount >= 1, "bytecode should contain at least 1 JMP")

        val mulCount = bytecode.instructions.count {
            it.op == dev.iris.core.bytecode.OpCode.MUL
        }
        assertTrue(mulCount >= 1, "bytecode should contain at least 1 MUL")

        val expectedOutput = "55\n385\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `complete benchmark scenario factorial`() {
        val source = """
            функция: целч факториал(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть 1;
                кц
                вернуть н * факториал(н - 1);
            кц

            целч ф0 = факториал(0);
            целч ф1 = факториал(1);
            целч ф2 = факториал(2);
            целч ф3 = факториал(3);
            целч ф4 = факториал(4);
            целч ф5 = факториал(5);
            целч ф6 = факториал(6);
            целч ф7 = факториал(7);
            целч ф8 = факториал(8);
            целч ф9 = факториал(9);
            целч ф10 = факториал(10);
            целч ф12 = факториал(12);
            печать:ф0;
            печать:ф1;
            печать:ф2;
            печать:ф3;
            печать:ф4;
            печать:ф5;
            печать:ф6;
            печать:ф7;
            печать:ф8;
            печать:ф9;
            печать:ф10;
            печать:ф12;
        """.trimIndent()

        val expectedOutput = "1\n1\n2\n6\n24\n120\n720\n5040\n40320\n362880\n3628800\n479001600\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `complete benchmark scenario array operations simulation`() {
        val source = """
            функция: целч заполнитьИСуммировать(целч размер)
            нч
                целч сумма = 0;
                целч к = 0;
                пока (к < размер)
                нч
                    сумма = сумма + (к * 10);
                    к = к + 1;
                кц
                вернуть сумма;
            кц

            целч сумма5 = заполнитьИСуммировать(5);
            целч сумма10 = заполнитьИСуммировать(10);
            целч сумма100 = заполнитьИСуммировать(100);
            печать:сумма5;
            печать:сумма10;
            печать:сумма100;
        """.trimIndent()

        val expectedOutput = "100\n450\n49500\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `complete benchmark scenario prime sieve with MOD`() {
        val source = """
            функция: бул простое(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть ложь;
                кц
                если (н <= 3)
                нч
                    вернуть правда;
                кц
                если (н % 2 == 0)
                нч
                    вернуть ложь;
                кц
                целч к = 3;
                пока (к * к <= н)
                нч
                    если (н % к == 0)
                    нч
                        вернуть ложь;
                    кц
                    к = к + 2;
                кц
                вернуть правда;
            кц

            функция: целч считатьПростые(целч предел)
            нч
                целч счет = 0;
                целч н = 2;
                пока (н <= предел)
                нч
                    если (простое(н))
                    нч
                        счет = счет + 1;
                    кц
                    н = н + 1;
                кц
                вернуть счет;
            кц

            целч простыхДо10 = считатьПростые(10);
            целч простыхДо50 = считатьПростые(50);
            целч простыхДо100 = считатьПростые(100);
            печать:простыхДо10;
            печать:простыхДо50;
            печать:простыхДо100;
        """.trimIndent()

        val expectedOutput = "4\n15\n25\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `complete benchmark scenario complex while loops`() {
        val source = """
            функция: целч вложенныеЦиклы(целч внешний, целч внутренний)
            нч
                целч итого = 0;
                целч к = 0;
                пока (к < внешний)
                нч
                    целч м = 0;
                    пока (м < внутренний)
                    нч
                        итого = итого + 1;
                        если (итого % 100 == 0)
                        нч
                            итого = итого + к;
                        кц
                        м = м + 1;
                    кц
                    к = к + 1;
                кц
                вернуть итого;
            кц

            целч р1 = вложенныеЦиклы(10, 10);
            целч р2 = вложенныеЦиклы(5, 20);
            целч р3 = вложенныеЦиклы(20, 5);
            печать:р1;
            печать:р2;
            печать:р3;
        """.trimIndent()

        val expectedOutput = "109\n104\n119\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `full pipeline verifies instruction count consistency`() {
        val source = """
            функция: целч тест(целч х)
            нч
                вернуть х + 1;
            кц

            целч р = тест(5);
            печать:р;
        """.trimIndent()

        val bytecode = compileAndGetBytecode(source)

        assertTrue(bytecode.instructions.isNotEmpty(), "bytecode should have instructions")
        assertTrue(bytecode.functions.isNotEmpty(), "bytecode should have functions")

        val hasHalt = bytecode.instructions.any {
            it.op == dev.iris.core.bytecode.OpCode.HALT
        }
        assertTrue(hasHalt, "bytecode should end with HALT")

        val expectedOutput = "6\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `combined benchmark all three scenarios together`() {
        val source = """
            функция: целч факториал(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть 1;
                кц
                вернуть н * факториал(н - 1);
            кц

            функция: целч суммаДиапазона(целч н)
            нч
                целч сумма = 0;
                целч к = 1;
                пока (к <= н)
                нч
                    сумма = сумма + к;
                    к = к + 1;
                кц
                вернуть сумма;
            кц

            функция: бул простое(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть ложь;
                кц
                целч к = 2;
                пока (к * к <= н)
                нч
                    если (н % к == 0)
                    нч
                        вернуть ложь;
                    кц
                    к = к + 1;
                кц
                вернуть правда;
            кц

            функция: целч считатьПростые(целч предел)
            нч
                целч счет = 0;
                целч н = 2;
                пока (н <= предел)
                нч
                    если (простое(н))
                    нч
                        счет = счет + 1;
                    кц
                    н = н + 1;
                кц
                вернуть счет;
            кц

            целч ф5 = факториал(5);
            целч ф10 = факториал(10);
            целч сумма10 = суммаДиапазона(10);
            целч сумма100 = суммаДиапазона(100);
            целч простыхДо20 = считатьПростые(20);
            целч простыхДо50 = считатьПростые(50);
            печать:ф5;
            печать:ф10;
            печать:сумма10;
            печать:сумма100;
            печать:простыхДо20;
            печать:простыхДо50;
        """.trimIndent()

        val bytecode = compileAndGetBytecode(source)

        assertEquals(5, bytecode.functions.size, "should have 5 functions (4 user + main)")

        val factorialFunc = bytecode.functions.find { it.name == "факториал" }
        assertNotNull(factorialFunc, "factorial function should exist")

        val sumFunc = bytecode.functions.find { it.name == "суммаДиапазона" }
        assertNotNull(sumFunc, "sum function should exist")

        val isPrimeFunc = bytecode.functions.find { it.name == "простое" }
        assertNotNull(isPrimeFunc, "isPrime function should exist")

        val countPrimesFunc = bytecode.functions.find { it.name == "считатьПростые" }
        assertNotNull(countPrimesFunc, "countPrimes function should exist")

        val expectedOutput = "120\n3628800\n55\n5050\n8\n15\n"

        val output = runAndCollectOutput(source)
        assertEquals(expectedOutput, output)
    }
}
