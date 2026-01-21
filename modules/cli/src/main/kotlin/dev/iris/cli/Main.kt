package dev.iris.cli

import dev.iris.compiler.Compiler
import dev.iris.jit.pipeline.BytecodeLowering
import dev.iris.jit.pipeline.PipelineJitCompiler
import dev.iris.jit.pipeline.VmIntegratedCodeEmitter
import dev.iris.jit.runtime.AsyncJit
import dev.iris.jit.runtime.asJitHooks
import dev.iris.jit.support.ProgramFunctionProvider
import dev.iris.parser.lexer.Lexer
import dev.iris.parser.parser.Parser
import dev.iris.vm.VirtualMachine
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

val knownArgs = listOf("--emit-bytecode", "--no-jit")

fun main(args: Array<String>) {
    val emitBytecodeOnly = args.contains("--emit-bytecode")
    val disableJit = args.contains("--no-jit")
    val sourceArg = args.firstOrNull { it !in knownArgs }
    val sourcePath = if (sourceArg != null) Path.of(sourceArg) else null
    val source = if (sourcePath != null) {
        if (!Files.exists(sourcePath)) {
            System.err.println("File not found: $sourcePath")
            exitProcess(2)
        }
        Files.readString(sourcePath)
    } else {
        "процедура главная()\nнч\n    печать:1;\n    печать:2;\n    печать:3;\nкц\n"
    }

    val lexResult = Lexer(source).tokenize()
    lexResult.diagnostics.forEach { d -> System.err.println("[lex] ${d.severity}: ${d.message}") }

    val parseResult = Parser(lexResult.tokens).parse()
    parseResult.diagnostics.forEach { d ->
        System.err.println("[parse] ${d.severity}: ${d.message}")
    }
    val ast = parseResult.program ?: exitProcess(1)

    val comp = Compiler.compile(ast)
    comp.diagnostics.forEach { d -> System.err.println("[compile] ${d.severity}: ${d.message}") }
    val bytecode = comp.program ?: exitProcess(1)

    if (emitBytecodeOnly) {
        val output = StringBuilder()
        output.appendLine("=== Байткод ===")
        bytecode.instructions.forEachIndexed { idx, instr ->
            val op = if (instr.operand != null) " ${instr.operand}" else ""
            output.appendLine("$idx: ${instr.op}$op")
        }
        if (bytecode.functions.isNotEmpty()) {
            output.appendLine()
            output.appendLine("=== Функции ===")
            bytecode.functions.forEach { func ->
                output.appendLine("${func.name}: startIp=${func.startIp}, params=${func.paramCount}, locals=${func.localCount}, returns=${func.returnsValue}")
            }
        }
        if (bytecode.constPool.isNotEmpty()) {
            output.appendLine()
            output.appendLine("=== Const pool ===")
            bytecode.constPool.forEachIndexed { idx, value -> output.appendLine("$idx: $value") }
        }
        val outputPath = if (sourcePath != null) {
            val filename = sourcePath.fileName.toString()
            val dot = filename.lastIndexOf('.')
            val base = if (dot > 0) filename.substring(0, dot) else filename
            sourcePath.resolveSibling("$base.ibc")
        } else {
            Path.of("bytecode.ibc")
        }
        Files.writeString(outputPath, output.toString(), StandardCharsets.UTF_8)
        System.err.println("Wrote bytecode to $outputPath")
        exitProcess(0)
    }

    val asyncJit = if (disableJit || bytecode.functions.isEmpty()) {
        null
    } else {
        val provider = ProgramFunctionProvider(bytecode)
        val jitCompiler = PipelineJitCompiler(BytecodeLowering(provider), VmIntegratedCodeEmitter())
        AsyncJit(compiler = jitCompiler, funcCount = bytecode.functions.size)
    }

    val vm = VirtualMachine(jit = asyncJit?.asJitHooks())
    val result = vm.run(bytecode)

    asyncJit?.close()
    exitProcess(result.exitCode)
}
