package dev.iris.core.ast

sealed interface Expr {
    data class IntLiteral(val value: Int) : Expr
    data class BoolLiteral(val value: Boolean) : Expr
    data object NullLiteral : Expr
    data class Var(val name: String) : Expr

    data class Binary(val left: Expr, val operator: Operator, val right: Expr) : Expr
    data class Unary(val operator: Operator, val expr: Expr) : Expr

    data class Call(val callee: String, val args: List<Expr>) : Expr

    data class FieldAccess(val target: Expr, val field: String) : Expr
    data class Deref(val expr: Expr) : Expr
    data class Addr(val expr: Expr) : Expr

    data class New(val type: TypeRef, val size: Expr?) : Expr

    data class Assign(val target: Expr, val expr: Expr) : Expr

    data class ArrayAccess(val array: Expr, val index: Expr) : Expr

    data class ArrayLiteral(val elements: List<Expr>) : Expr
}