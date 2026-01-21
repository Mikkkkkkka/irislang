package dev.iris.compiler

import dev.iris.parser.lexer.Lexer
import dev.iris.parser.parser.Parser
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimpleParseTest {

    @Test
    fun `parse simple variable`() {
        val source = "целч а = 5;"
        val lexResult = Lexer(source).tokenize()
        assertTrue(lexResult.diagnostics.isEmpty(), "Lexer errors: ${lexResult.diagnostics}")

        val parseResult = Parser(lexResult.tokens).parse()
        assertNotNull(parseResult.program, "Parse failed: ${parseResult.diagnostics}")
        println("Parsed: ${parseResult.program}")
    }

    @Test
    fun `parse simple while`() {
        val source = """
            целч и = 0;
            пока (и < 3)
            нч
                и = и + 1;
            кц
        """.trimIndent()

        val lexResult = Lexer(source).tokenize()
        println("Tokens: ${lexResult.tokens}")
        assertTrue(lexResult.diagnostics.isEmpty(), "Lexer errors: ${lexResult.diagnostics}")

        val parseResult = Parser(lexResult.tokens).parse()
        println("Parse result: ${parseResult.program}")
        println("Parse diagnostics: ${parseResult.diagnostics}")
        assertNotNull(parseResult.program, "Parse failed: ${parseResult.diagnostics}")
    }

    @Test
    fun `compile simple while without VM`() {
        val source = """
            целч счетчик = 0;
            пока (счетчик < 3)
            нч
                счетчик = счетчик + 1;
            кц
        """.trimIndent()

        val lexResult = Lexer(source).tokenize()
        assertTrue(lexResult.diagnostics.isEmpty(), "Lexer errors: ${lexResult.diagnostics}")

        val output = StringBuilder()
        output.appendLine("=== Tokens ===")
        lexResult.tokens.forEach { output.appendLine("  ${it.kind}: '${it.lexeme}'") }

        val parseResult = Parser(lexResult.tokens).parse()
        output.appendLine("\n=== Parse Diagnostics ===")
        parseResult.diagnostics.forEach { output.appendLine("  $it") }
        assertNotNull(parseResult.program, "Parse failed: ${parseResult.diagnostics}")
        output.appendLine("=== AST ===")
        output.appendLine("Declarations: ${parseResult.program!!.declarations.size}")
        parseResult.program!!.declarations.forEach { output.appendLine("  $it") }
        output.appendLine("Statements: ${parseResult.program!!.statements.size}")
        parseResult.program!!.statements.forEach { output.appendLine("  $it") }

        val compileResult = Compiler.compile(parseResult.program!!)
        assertNotNull(compileResult.program, "Compile failed: ${compileResult.diagnostics}")

        output.appendLine("\n=== Bytecode ===")
        compileResult.program!!.instructions.forEachIndexed { idx, instr ->
            val op = if (instr.operand != null) " ${instr.operand}" else ""
            output.appendLine("$idx: ${instr.op}$op")
        }
        val tmpFile = java.io.File(System.getProperty("java.io.tmpdir"), "bytecode.txt")
        tmpFile.writeText(output.toString())
    }

    @Test
    fun `debug boolean function return`() {
        val source = """
            функция: бул тест()
            нч
                вернуть правда;
            кц

            функция: целч проверка(бул б)
            нч
                если (б)
                нч
                    вернуть 1;
                кц
                вернуть 0;
            кц

            печать:(проверка(тест()));
        """.trimIndent()

        val lexResult = Lexer(source).tokenize()
        assertTrue(lexResult.diagnostics.isEmpty(), "Lexer errors: ${lexResult.diagnostics}")

        val parseResult = Parser(lexResult.tokens).parse()
        assertNotNull(parseResult.program, "Parse failed: ${parseResult.diagnostics}")

        val compileResult = Compiler.compile(parseResult.program!!)
        assertNotNull(compileResult.program, "Compile failed: ${compileResult.diagnostics}")

        println("=== Functions ===")
        compileResult.program!!.functions.forEach { func ->
            println("${func.name}: startIp=${func.startIp}, params=${func.paramCount}, locals=${func.localCount}, returns=${func.returnsValue}")
        }

        println("\n=== Bytecode ===")
        compileResult.program!!.instructions.forEachIndexed { idx, instr ->
            val op = if (instr.operand != null) " ${instr.operand}" else ""
            println("$idx: ${instr.op}$op")
        }

        println("\n=== Running ===")
        val output = mutableListOf<String>()
        val vm = dev.iris.vm.VirtualMachine()
        vm.run(compileResult.program!!) { s ->
            output.add(s)
            println("OUTPUT: $s")
        }

        println("\n=== Collected output: $output ===")
        kotlin.test.assertEquals("1", output[0], "Boolean function should return true, проверка should return 1")
    }

    @Test
    fun `debug else-if construct`() {
        val source = """
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

            печать:(условия(50));
        """.trimIndent()

        val lexResult = Lexer(source).tokenize()
        assertTrue(lexResult.diagnostics.isEmpty(), "Lexer errors: ${lexResult.diagnostics}")

        val parseResult = Parser(lexResult.tokens).parse()
        assertNotNull(parseResult.program, "Parse failed: ${parseResult.diagnostics}")

        println("=== AST ===")
        parseResult.program!!.declarations.forEach { println("  $it") }

        val compileResult = Compiler.compile(parseResult.program!!)
        assertNotNull(compileResult.program, "Compile failed: ${compileResult.diagnostics}")

        println("\n=== Bytecode ===")
        compileResult.program!!.instructions.forEachIndexed { idx, instr ->
            val op = if (instr.operand != null) " ${instr.operand}" else ""
            println("$idx: ${instr.op}$op")
        }

        println("\n=== Running ===")
        val output = mutableListOf<String>()
        val vm = dev.iris.vm.VirtualMachine()
        vm.run(compileResult.program!!) { s ->
            output.add(s)
            println("OUTPUT: $s")
        }

        println("\n=== Collected output: $output ===")
        kotlin.test.assertEquals("2", output[0], "условия(50) should return 2")
    }

    @Test
    fun `debug sequential if without else`() {
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
                вернуть ложь;
            кц

            функция: целч прБул(бул б)
            нч
                если (б)
                нч
                    вернуть 1;
                кц
                вернуть 0;
            кц

            печать:(прБул(простое(2)));
        """.trimIndent()

        val lexResult = Lexer(source).tokenize()
        assertTrue(lexResult.diagnostics.isEmpty(), "Lexer errors: ${lexResult.diagnostics}")

        val parseResult = Parser(lexResult.tokens).parse()
        assertNotNull(parseResult.program, "Parse failed: ${parseResult.diagnostics}")

        println("=== AST ===")
        parseResult.program!!.declarations.forEach { println("  $it") }

        val compileResult = Compiler.compile(parseResult.program!!)
        assertNotNull(compileResult.program, "Compile failed: ${compileResult.diagnostics}")

        println("\n=== Functions ===")
        compileResult.program!!.functions.forEach { func ->
            println("${func.name}: startIp=${func.startIp}, params=${func.paramCount}, locals=${func.localCount}, returns=${func.returnsValue}")
        }

        println("\n=== Bytecode ===")
        compileResult.program!!.instructions.forEachIndexed { idx, instr ->
            val op = if (instr.operand != null) " ${instr.operand}" else ""
            println("$idx: ${instr.op}$op")
        }

        println("\n=== Running ===")
        val output = mutableListOf<String>()
        val vm = dev.iris.vm.VirtualMachine()
        vm.run(compileResult.program!!) { s ->
            output.add(s)
            println("OUTPUT: $s")
        }

        println("\n=== Collected output: $output ===")
        kotlin.test.assertEquals("1", output[0], "простое(2) should return true, прБул should return 1")
    }
}
