package dev.iris.core.ast

sealed interface Expr {
    data class IntLiteral(val value: Int) : Expr
    data class BoolLiteral(val value: Boolean) : Expr
    data class Var(val name: String) : Expr

    data class BinaryExpr(val left: Expr, val operator: Operator, val right: Expr) : Expr
    data class UnaryExpr(val operator: Operator, val expr: Expr) : Expr

    data class Call(val callee: String, val args: List<Expr>) : Expr

    data class FieldAccess(val target: Expr, val field: String) : Expr

    data class New(val type: TypeRef, val expr: Expr?) : Expr
}