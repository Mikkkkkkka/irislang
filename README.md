# IRIS (Information Systems) — multi-module Kotlin project

This is a starter multi-module project for your language:

- `core` — shared types (AST, bytecode, diagnostics)
- `parser` — lexer + parser -> AST
- `compiler` — AST -> bytecode
- `vm` — bytecode interpreter
- `jit` — async JIT placeholder (compile in background, fallback to interpreter)
- `cli` — command-line runner

## Requirements
- JDK 21
- Kotlin 2.2.21 (via Gradle plugin)

## Quick start

Generate Gradle wrapper (recommended):
```bash
gradle wrapper --gradle-version 8.10.2
```

Run demo (no args):
```bash
./gradlew :cli:run
```

Run a file:
```bash
./gradlew :cli:run --args="examples/hello.is"
```
