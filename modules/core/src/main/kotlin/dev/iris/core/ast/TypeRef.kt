package dev.iris.core.ast

interface TypeRef {
    data object IntType : TypeRef
    data object BoolType: TypeRef
    data class StructType(val name: String): TypeRef
    data class Pointer(val base: TypeRef): TypeRef
}