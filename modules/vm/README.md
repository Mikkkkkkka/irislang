# IRIS Virtual Machine

A stack-based virtual machine for the IRIS programming language with support for asynchronous JIT compilation.

## Architecture

The IRIS VM is a single-threaded stack-based interpreter with the following components:

### Core Components

- **Operand Stack**: Expression evaluation using LIFO stack
- **Call Stack**: Function call frames with local variables
- **Heap**: Dynamic memory for arrays and structs
- **Garbage Collector**: Mark-sweep GC with automatic triggering
- **Global Variables**: Shared mutable state across functions

### Memory Model

#### Value Types
```kotlin
sealed class Value {
    data class Int(val value: Long)        // 64-bit integers
    data class Bool(val value: Boolean)    // Booleans
    data class HeapRef(val address: Long)  // Heap references
}
```

#### Heap Objects
```kotlin
sealed class HeapObject {
    data class Array(val size: Int, val elements: Array<Value>)
    data class Struct(val typeIndex: Int, val fields: Array<Value>)
}
```

#### Call Frames
```kotlin
data class CallFrame(
    val funcIndex: Int,      // Function being executed
    val returnIp: Int,       // Return address
    val locals: Array<Value>,// Local variables
    val basePointer: Int     // Stack base pointer
)
```

## Instruction Set

The VM implements 50+ instructions organized into categories:

### Constants
- `PUSH_I64 <value>` - Push 64-bit integer constant
- `PUSH_TRUE` - Push boolean true
- `PUSH_FALSE` - Push boolean false

### Arithmetic
- `ADD` - Pop b, pop a, push a+b
- `SUB` - Pop b, pop a, push a-b
- `MUL` - Pop b, pop a, push a*b
- `DIV` - Pop b, pop a, push a/b
- `MOD` - Pop b, pop a, push a%b
- `NEG` - Pop a, push -a

### Comparison
- `CMP_EQ` - Pop b, pop a, push a==b
- `CMP_NE` - Pop b, pop a, push a!=b
- `CMP_LT` - Pop b, pop a, push a<b
- `CMP_LE` - Pop b, pop a, push a<=b
- `CMP_GT` - Pop b, pop a, push a>b
- `CMP_GE` - Pop b, pop a, push a>=b

### Logical
- `AND` - Pop b, pop a, push a&&b
- `OR` - Pop b, pop a, push a||b
- `NOT` - Pop a, push !a

### Control Flow
- `JMP <target>` - Unconditional jump to instruction
- `JMP_IF_FALSE <target>` - Pop condition, jump if false
- `JMP_IF_TRUE <target>` - Pop condition, jump if true

### Variables
- `LOAD_LOCAL <index>` - Push local variable
- `STORE_LOCAL <index>` - Pop value, store to local
- `LOAD_GLOBAL <index>` - Push global variable
- `STORE_GLOBAL <index>` - Pop value, store to global

### Functions
- `CALL <funcIndex>` - Call function (may use JIT-compiled version)
- `RET` - Return from function with value
- `RET_VOID` - Return from function without value

### Arrays
- `ALLOC_ARR` - Pop size, allocate array, push reference
- `LOAD_ARR` - Pop index, pop array_ref, push element
- `STORE_ARR` - Pop value, pop index, pop array_ref, store element

### Structs
- `ALLOC_STRUCT <typeIndex>` - Pop field count, allocate struct
- `LOAD_FIELD <fieldIndex>` - Pop struct_ref, push field value
- `STORE_FIELD <fieldIndex>` - Pop value, pop struct_ref, store field
- `NEW <typeIndex>` - Alias for ALLOC_STRUCT

### I/O
- `PRINT_I64` - Pop i64, print it
- `PRINT_BOOL` - Pop bool, print it

### Stack Manipulation
- `POP` - Discard top of stack
- `DUP` - Duplicate top of stack

### Program Control
- `HALT` - Stop execution

## Garbage Collection

The VM uses a mark-sweep garbage collector that automatically triggers when:
- Allocation count exceeds threshold (default: 100 allocations)
- Manually via `collectGarbage()`

### GC Algorithm
1. **Mark Phase**: Traverse from roots (stack, call frames, globals) and mark reachable objects
2. **Sweep Phase**: Free all unmarked objects

## JIT Integration

The VM supports optional JIT compilation through the `JitHooks` interface:

```kotlin
interface JitHooks {
    fun getCompiled(funcIndex: Int): CompiledCode?
    fun notifyCall(funcIndex: Int)
}

interface CompiledCode {
    fun execute(vm: VirtualMachine)
}
```

### JIT Flow
1. On function call, VM checks if compiled version exists
2. If available, executes compiled code
3. Otherwise, interprets bytecode and notifies JIT
4. JIT can compile hot functions asynchronously
5. Future calls use compiled version

The VM remains independent of the JIT module - the JIT module implements `JitHooks` and depends on VM.

## Usage

### Basic Execution
```kotlin
val program = BytecodeProgram(
    instructions = listOf(
        Instr(OpCode.PUSH_I64, 42),
        Instr(OpCode.PRINT_I64),
        Instr(OpCode.HALT)
    )
)

val vm = VirtualMachine()
vm.run(program) // Prints: 42
```

### With Functions
```kotlin
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

vm.run(program) // Prints: 30
```

### With JIT
```kotlin
// JIT implementation from jit module
val jitHooks: JitHooks = createAsyncJitHooks()
val vm = VirtualMachine(
    jit = jitHooks,
    gcTriggerThreshold = 100
)
vm.run(program)
```

### Custom Output
```kotlin
val output = mutableListOf<String>()
vm.run(program) { line ->
    output.add(line)
}
```

## Testing

The VM has comprehensive test coverage:
- **Unit Tests**: 40+ tests covering individual instructions and components
- **Integration Tests**: End-to-end scenarios with multiple features
- **Performance Tests**: GC triggering and memory management

Run tests:
```bash
./gradlew :vm:test
```

## Implementation Details

### Stack-Based Execution
The VM uses a stack-based architecture for simplicity and ease of code generation:
- All operations work with stack values
- No register allocation needed
- Natural expression evaluation order

### Automatic GC Triggering
GC automatically triggers every N allocations (configurable):
```kotlin
private fun trackAllocation() {
    allocationsSinceLastGc++
    if (allocationsSinceLastGc >= gcTriggerThreshold) {
        collectGarbage()
        allocationsSinceLastGc = 0
    }
}
```

### Jump Instructions
Jumps set `ip = target - 1` to account for `ip++` at end of loop:
```kotlin
OpCode.JMP -> {
    val target = instr.operand ?: error("Missing operand for JMP")
    ip = target.toInt() - 1  // -1 because ip++ at end of loop
}
```

## Performance Characteristics

- **Interpretation**: ~100-1000x slower than native code
- **JIT Compilation**: Can approach native performance for hot loops
- **GC Pause**: Mark-sweep causes stop-the-world pauses
- **Memory**: Heap grows dynamically, GC reclaims unused memory

## Future Enhancements

Potential improvements:
- Generational GC for better pause times
- Inline caching for faster field access
- Stack maps for precise GC
- Tiered compilation (interpreter → baseline JIT → optimizing JIT)
- SIMD instruction support
- Multi-threading support

## License

Part of the IRIS programming language project.
