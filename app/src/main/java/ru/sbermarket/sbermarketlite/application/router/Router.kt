package ru.sbermarket.sbermarketlite.application.router

sealed interface BackStack<Screen>

internal data class InternalBackStack<Screen>(
    val head: Screen,
    val tail: List<Screen>
): BackStack<Screen>

fun <Screen> BackStack(
    current: Screen,
    previous: List<Screen> = listOf()
): BackStack<Screen> {
    return InternalBackStack(
        head = current,
        tail = previous
    )
}

fun <Screen> BackStack<Screen>.top(): Screen {
    return when (this) {
        is InternalBackStack -> head
    }
}

fun <Screen> BackStack<Screen>.replace(screen: Screen): BackStack<Screen> {
    return when (this) {
        is InternalBackStack -> copy(head = screen)
    }
}

fun <Screen> BackStack<Screen>.push(screen: Screen): BackStack<Screen> {
    return when (this) {
        is InternalBackStack -> copy(head = screen, tail = tail.plus(head))
    }
}

fun <Screen> BackStack<Screen>.pop(): Pair<BackStack<Screen>, Screen?> {
    return when (this) {
        is InternalBackStack -> {
            if (tail.isEmpty()) {
                this to null
            } else {
                this.copy(
                    head = tail.last(),
                    tail = tail.dropLast(1)
                ) to head
            }
        }
    }
}

fun <Screen> BackStack<Screen>.clear(): BackStack<Screen> {
    return when (this) {
        is InternalBackStack -> {
            copy(tail = listOf())
        }
    }
}


/***
 * Tabs
 *
 */


sealed interface Tabs<Screen>


internal data class InternalTabs<Screen>(
    val current: String,
    val tabs: Map<String, BackStack<Screen>>
): Tabs<Screen>

fun <Screen> Tabs<Screen>.currentTab(): BackStack<Screen>? {
    return when (this) {
        is InternalTabs -> {
            tabs[current]
        }
    }
}


fun <Screen> Tabs<Screen>.mapSelected(t: (BackStack<Screen>) -> BackStack<Screen>): Tabs<Screen> {
    return when (this) {
        is InternalTabs -> {
            copy(
                tabs = tabs.map { entry ->
                    if (isSelected(entry.key)) {
                        entry.key to t(entry.value)
                    } else {
                        entry.key to entry.value
                    }
                }.toMap()
            )
        }
    }
}

fun <Screen> Tabs<Screen>.isSelected(id: String): Boolean {
    return when (this) {
        is InternalTabs -> current == id
    }
}

val <Screen> Tabs<Screen>.current: Screen?
    get() = currentTab()?.top()

fun <Screen> Tabs<Screen>.clearAll(): Tabs<Screen> {
    return when (this) {
        is InternalTabs -> {
            val nextTabs = tabs.map { entry ->
                entry.key to entry.value.clear()
            }.toMap()

            copy(tabs = nextTabs)
        }
    }
}

fun <Screen> Tabs<Screen>.select(id: String): Tabs<Screen> {
    return when (this) {
        is InternalTabs -> {
            copy(
                current = id
            )
        }
    }
}

