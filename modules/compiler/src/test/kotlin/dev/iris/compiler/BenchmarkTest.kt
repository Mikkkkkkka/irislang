package dev.iris.compiler

import dev.iris.parser.lexer.Lexer
import dev.iris.parser.parser.Parser
import dev.iris.vm.VirtualMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BenchmarkTest {

    private fun parseCompileRun(source: String, collectOutput: Boolean = false): Pair<Long, List<String>> {
        val output = mutableListOf<String>()
        val printer: (String) -> Unit = if (collectOutput) { s -> output.add(s) } else { {} }

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
        val startTime = System.currentTimeMillis()
        vm.run(compileResult.program!!, printer)
        val endTime = System.currentTimeMillis()

        return Pair(endTime - startTime, output)
    }

    @Test
    fun `benchmark factorial 20 recursive`() {
        val source = """
            функция: целч факториал(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть 1;
                кц
                вернуть н * факториал(н - 1);
            кц

            целч результат = факториал(20);
            печать:(результат);
        """.trimIndent()

        val (time, output) = parseCompileRun(source, collectOutput = true)
        assertEquals("2432902008176640000", output[0], "Factorial of 20 should be 2432902008176640000")
        println("Factorial(20) recursive: ${output[0]}, time: ${time}ms")
    }

    @Test
    fun `benchmark factorial 20 iterative`() {
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

            целч результат = факториалИтер(20);
            печать:(результат);
        """.trimIndent()

        val (time, output) = parseCompileRun(source, collectOutput = true)
        assertEquals("2432902008176640000", output[0], "Factorial of 20 should be 2432902008176640000")
        println("Factorial(20) iterative: ${output[0]}, time: ${time}ms")
    }

    @Test
    fun `benchmark bubble sort 10000 elements`() {
        val source = """
            процедура пузырьковаяСортировка(целч* массив, целч размер)
            нч
                целч индекс1 = 0;
                пока (индекс1 < размер - 1)
                нч
                    целч индекс2 = 0;
                    пока (индекс2 < размер - индекс1 - 1)
                    нч
                        если (массив[индекс2] > массив[индекс2 + 1])
                        нч
                            целч врем = массив[индекс2];
                            массив[индекс2] = массив[индекс2 + 1];
                            массив[индекс2 + 1] = врем;
                        кц
                        индекс2 = индекс2 + 1;
                    кц
                    индекс1 = индекс1 + 1;
                кц
            кц

            функция: бул отсортирован(целч* массив, целч размер)
            нч
                целч индекс = 0;
                пока (индекс < размер - 1)
                нч
                    если (массив[индекс] > массив[индекс + 1])
                    нч
                        вернуть ложь;
                    кц
                    индекс = индекс + 1;
                кц
                вернуть правда;
            кц

            целч размер = 10000;
            целч* числа = новый:целч:(размер);

            целч заполн = 0;
            пока (заполн < размер)
            нч
                числа[заполн] = размер - заполн;
                заполн = заполн + 1;
            кц

            пузырьковаяСортировка(числа, размер);

            функция: целч бул2цел(бул б)
            нч
                если (б)
                нч
                    вернуть 1;
                кц
                вернуть 0;
            кц

            печать:(бул2цел(отсортирован(числа, размер)));
            печать:(числа[0]);
            печать:(числа[размер - 1]);
        """.trimIndent()

        val (time, output) = parseCompileRun(source, collectOutput = true)
        assertEquals("1", output[0], "Array should be sorted")
        assertEquals("1", output[1], "First element should be 1")
        assertEquals("10000", output[2], "Last element should be 10000")
        println("Bubble sort 10000 elements: sorted=${output[0]}, first=${output[1]}, last=${output[2]}, time: ${time}ms")
    }

    @Test
    fun `benchmark prime sieve simulation 100000`() {
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

            целч результат = считатьПростые(100000);
            печать:(результат);
        """.trimIndent()

        val (time, output) = parseCompileRun(source, collectOutput = true)
        assertEquals("9592", output[0], "There should be 9592 primes up to 100000")
        println("Prime count up to 100000: ${output[0]}, time: ${time}ms")
    }

    @Test
    fun `verify factorial correctness for small values`() {
        val source = """
            функция: целч факт(целч н)
            нч
                если (н <= 1)
                нч
                    вернуть 1;
                кц
                вернуть н * факт(н - 1);
            кц

            печать:(факт(0));
            печать:(факт(1));
            печать:(факт(2));
            печать:(факт(3));
            печать:(факт(4));
            печать:(факт(5));
            печать:(факт(10));
        """.trimIndent()

        val (_, output) = parseCompileRun(source, collectOutput = true)

        assertEquals("1", output[0], "0! = 1")
        assertEquals("1", output[1], "1! = 1")
        assertEquals("2", output[2], "2! = 2")
        assertEquals("6", output[3], "3! = 6")
        assertEquals("24", output[4], "4! = 24")
        assertEquals("120", output[5], "5! = 120")
        assertEquals("3628800", output[6], "10! = 3628800")
    }

    @Test
    fun `verify prime checker correctness`() {
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

            функция: целч прБул(бул б)
            нч
                если (б)
                нч
                    вернуть 1;
                кц
                вернуть 0;
            кц

            печать:(прБул(простое(0)));
            печать:(прБул(простое(1)));
            печать:(прБул(простое(2)));
            печать:(прБул(простое(3)));
            печать:(прБул(простое(4)));
            печать:(прБул(простое(5)));
            печать:(прБул(простое(7)));
            печать:(прБул(простое(9)));
            печать:(прБул(простое(11)));
            печать:(прБул(простое(97)));
        """.trimIndent()

        val (_, output) = parseCompileRun(source, collectOutput = true)

        assertEquals("0", output[0], "0 is not prime")
        assertEquals("0", output[1], "1 is not prime")
        assertEquals("1", output[2], "2 is prime")
        assertEquals("1", output[3], "3 is prime")
        assertEquals("0", output[4], "4 is not prime")
        assertEquals("1", output[5], "5 is prime")
        assertEquals("1", output[6], "7 is prime")
        assertEquals("0", output[7], "9 is not prime")
        assertEquals("1", output[8], "11 is prime")
        assertEquals("1", output[9], "97 is prime")
    }

    @Test
    fun `verify bubble sort comparison count`() {
        val source = """
            функция: целч сравнения(целч н)
            нч
                целч счет = 0;
                целч к = 0;
                пока (к < н - 1)
                нч
                    целч м = 0;
                    пока (м < н - к - 1)
                    нч
                        счет = счет + 1;
                        м = м + 1;
                    кц
                    к = к + 1;
                кц
                вернуть счет;
            кц

            печать:(сравнения(5));
            печать:(сравнения(10));
            печать:(сравнения(100));
        """.trimIndent()

        val (_, output) = parseCompileRun(source, collectOutput = true)

        assertEquals("10", output[0], "5 elements: 10 comparisons")
        assertEquals("45", output[1], "10 elements: 45 comparisons")
        assertEquals("4950", output[2], "100 elements: 4950 comparisons")
    }

    @Test
    fun `demonstrate all language features`() {
        val source = """
            функция: целч арифметика(целч а, целч б)
            нч
                целч сумма = а + б;
                целч разн = а - б;
                целч произв = а * б;
                целч частн = а / б;
                целч остат = а % б;
                вернуть сумма + разн + произв + частн + остат;
            кц

            функция: целч условия(целч х)
            нч
                если (х < 0)
                нч
                    вернуть 0;
                кц
                иначе если (х < 10)
                нч
                    вернуть 1;
                кц
                иначе если (х < 100)
                нч
                    вернуть 2;
                кц
                вернуть 3;
            кц

            функция: целч циклы(целч н)
            нч
                целч сумма = 0;
                целч к = 0;
                пока (к < н)
                нч
                    если (к % 2 == 0)
                    нч
                        сумма = сумма + к;
                    кц
                    к = к + 1;
                кц
                вернуть сумма;
            кц

            функция: целч рекурсия(целч н)
            нч
                если (н <= 0)
                нч
                    вернуть 0;
                кц
                вернуть н + рекурсия(н - 1);
            кц

            функция: целч циклДля(целч предел)
            нч
                целч сумма = 0;
                для (целч счетчик от 0 до предел)
                нч
                    сумма = сумма + счетчик;
                кц
                вернуть сумма;
            кц

            печать:(арифметика(20, 3));
            печать:(условия(50));
            печать:(циклы(20));
            печать:(рекурсия(10));
            печать:(циклДля(10));
        """.trimIndent()

        val (_, output) = parseCompileRun(source, collectOutput = true)

        assertEquals("108", output[0], "Arithmetic: (20+3)+(20-3)+(20*3)+(20/3)+(20%3) = 23+17+60+6+2 = 108")
        assertEquals("2", output[1], "Conditions result: 50 is in [10,100) range")
        assertEquals("90", output[2], "While loop result: 0+2+4+6+8+10+12+14+16+18=90")
        assertEquals("55", output[3], "Recursion result: 1+2+...+10=55")
        assertEquals("45", output[4], "For loop result: 0+1+2+3+4+5+6+7+8+9=45")

        println("All language features verified successfully!")
    }
}
