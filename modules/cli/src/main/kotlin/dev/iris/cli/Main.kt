package dev.iris.cli

import dev.iris.compiler.Compiler
import dev.iris.jit.pipeline.BaselineCodeEmitter
import dev.iris.jit.pipeline.BytecodeLowering
import dev.iris.jit.pipeline.PipelineJitCompiler
import dev.iris.jit.runtime.AsyncJit
import dev.iris.jit.support.SingleFunctionProvider
import dev.iris.parser.lexer.Lexer
import dev.iris.parser.parser.Parser
import dev.iris.vm.VirtualMachine
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val source = if (args.isNotEmpty()) {
        val p = Path.of(args[0])
        if (!Files.exists(p)) {
            System.err.println("File not found: $p")
            exitProcess(2)
        }
        Files.readString(p)
    } else {
        "печать:1;\nпечать:2;\nпечать:3;\n"
    }

    val lexResult = Lexer(source).tokenize()
    lexResult.diagnostics.forEach { d -> System.err.println("[lex] ${d.severity}: ${d.message}") }

    val parseResult = Parser(lexResult.tokens).parse()
    parseResult.diagnostics.forEach { d -> System.err.println("[parse] ${d.severity}: ${d.message}") }
    val ast = parseResult.program ?: exitProcess(1)

    val comp = Compiler.compile(ast)
    comp.diagnostics.forEach { d -> System.err.println("[compile] ${d.severity}: ${d.message}") }
    val bytecode = comp.program ?: exitProcess(1)

    val provider = SingleFunctionProvider(bytecode)
    val jitCompiler = PipelineJitCompiler(
        BytecodeLowering(provider),
        BaselineCodeEmitter()
    )
    val jit = AsyncJit(compiler = jitCompiler, funcCount = 1)
    jit.ensureCompilation(funcIndex = 0)

    val vm = VirtualMachine()
    val result = vm.run(bytecode)

    val compiled = jit.compiled(0)
    if (compiled != null) {
        System.err.println("[jit] ready (hook it into CALL dispatch later)")
    } else {
        System.err.println("[jit] not ready yet (expected for tiny programs)")
    }

    jit.close()
    exitProcess(result.exitCode)
}
