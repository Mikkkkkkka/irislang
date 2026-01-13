package dev.iris.jit.runtime

class StackUnderflowException(op: String) : IllegalStateException("Stack underflow at $op")
