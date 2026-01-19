package dev.iris.core.ast

sealed interface Decl {
    data class Function(
        val name: String,
        val returnType: TypeRef,
        val params: List<Stmt.VarDecl>,
        val body: Stmt.Block
    ): Decl

    data class Procedure(
        val name: String,
        val params: List<Stmt.VarDecl>,
        val body: Stmt.Block
    ): Decl

    data class Structure (
        val name: String,
        val fields: List<Stmt.VarDecl>,
    ) : Decl
}