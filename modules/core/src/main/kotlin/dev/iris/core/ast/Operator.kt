package dev.iris.core.ast

enum class Operator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    REMAINING,

    AND,
    OR,
    NOT,
    EQUAL,
    NOT_EQUAL,
    GREATER,
    LESS,
    GREATER_EQUAL,
    LESS_EQUAL,

    NEGATIVE,
    ADDRESS,
    DEREFERENCE,
}