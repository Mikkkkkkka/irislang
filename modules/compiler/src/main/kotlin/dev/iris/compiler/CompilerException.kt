package dev.iris.compiler

sealed class CompilerException(message: String) : Exception(message)

class UndefinedSymbolException(name: String) :
    CompilerException("Undefined symbol: $name")

class DuplicateSymbolException(name: String) :
    CompilerException("Symbol already defined: $name")

class TypeMismatchException(expected: String, actual: String) :
    CompilerException("Type mismatch: expected $expected, got $actual")

class InvalidOperationException(message: String) :
    CompilerException(message)

class UnresolvedLabelException(address: Int) :
    CompilerException("Unresolved label at address: $address")

class BreakOutsideLoopException :
    CompilerException("Break statement outside of loop")

class ContinueOutsideLoopException :
    CompilerException("Continue statement outside of loop")
