package dev.iris.cli

import dev.iris.compiler.Compiler
import dev.iris.jit.AsyncJit
import dev.iris.parser.Parser
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
        // Demo program if no args
        "print 1\nprint 2\nprint 3\n"
    }

    val parse = Parser.parse(source)
    parse.diagnostics.forEach { d -> System.err.println("[parse] ${d.severity}: ${d.message}") }
    val ast = parse.program ?: exitProcess(1)

    val comp = Compiler.compile(ast)
    comp.diagnostics.forEach { d -> System.err.println("[compile] ${d.severity}: ${d.message}") }
    val bytecode = comp.program ?: exitProcess(1)

    // Async JIT protocol demo: "compile" bytecode in background while we still run interpreter.
    val jit = AsyncJit()
    val key = "main"
    jit.ensureCompilation(key, bytecode)

    val vm = VirtualMachine()
    val result = vm.run(bytecode)

    val compiled = jit.getIfReady(key)
    if (compiled != null) {
        System.err.println("[jit] ready: ${compiled.id} (hook it into CALL dispatch later)")
    } else {
        System.err.println("[jit] not ready yet (expected for tiny programs)")
    }

    jit.close()
    exitProcess(result.exitCode)
}
