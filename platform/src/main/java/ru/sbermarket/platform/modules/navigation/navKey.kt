package ru.sbermarket.platform.modules.navigation


sealed interface NavKey {
    companion object {
        var count: Int = 0
        fun getNext(): Int {
            count =+ 1
            return count
        }
    }
}

fun NavKey.toBox(): BoxNavKey {
    return when (this) {
        is ScreenNavKey -> this.parent
        is StackNavKey -> this
        is TabNavKey3 -> this
        is TabNavKey5 -> this
    }
}

sealed interface BoxNavKey : NavKey

data class TabNavKey3 internal constructor(
    internal val value : String = NavKey.getNext().toString()
): BoxNavKey

data class TabNavKey5 internal constructor(
    internal val value : String = NavKey.getNext().toString()
): BoxNavKey
data class StackNavKey internal constructor(
    internal val value : String = NavKey.getNext().toString()
) : BoxNavKey


data class ScreenNavKey internal constructor(
    internal val parent: BoxNavKey,
    internal val value : String = NavKey.getNext().toString()
): NavKey

val NavKey.value: String
    get() = when (this) {
        is TabNavKey3    -> value
        is TabNavKey5    -> value
        is StackNavKey   -> value
        is ScreenNavKey  -> value
    }
