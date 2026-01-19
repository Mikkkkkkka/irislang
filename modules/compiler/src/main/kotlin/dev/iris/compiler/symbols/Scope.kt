package dev.iris.compiler.symbols

class Scope(
    val parent: Scope? = null,
    val isFunction: Boolean = false
) {
    private val symbols = mutableMapOf<String, Symbol>()
    private var nextLocalIndex = if (isFunction) 0 else (parent?.localCount() ?: 0)

    fun define(symbol: Symbol): Symbol {
        symbols[symbol.name] = symbol
        return symbol
    }

    fun resolve(name: String): Symbol? =
        symbols[name] ?: parent?.resolve(name)

    fun resolveLocal(name: String): Symbol? =
        symbols[name]

    fun allocateLocal(): Int {
        val index = nextLocalIndex
        nextLocalIndex++
        return index
    }

    fun localCount(): Int = nextLocalIndex

    fun isDefined(name: String): Boolean =
        symbols.containsKey(name)

    fun propagateLocalCountToParent() {
        parent?.updateLocalCount(nextLocalIndex)
    }

    private fun updateLocalCount(count: Int) {
        if (count > nextLocalIndex) {
            nextLocalIndex = count
        }
    }
}
