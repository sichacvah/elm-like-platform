package ru.sbermarket.platform.modules.navigation

import ru.sbermarket.platform.modules.json.JsonNull
import ru.sbermarket.platform.modules.json.JsonType

sealed interface BackStack : Destination
internal data class InternalBackStack(
    val stack: ZipList<NavKey>,
    override val id: String,
    override val params: JsonType,
    override val key: BoxNavKey
): BackStack

fun BackStack.top(): NavKey {
    return stack().head()
}

fun BackStack.push(destination: Destination): BackStack {
    val stack = stack().push(destination.key)
    return backstack(
        id = id(),
        params = params(),
        key = key(),
        stack = stack
    )
}

fun BackStack.clear(): BackStack {
    return backstack(
        id = id(),
        params = params(),
        key = key(),
        stack = stack().clear()
    )
}

fun BackStack.pop(): Pair<BackStack, NavKey?> {
    val (stack, navKey) = stack().pop()

    return backstack(
        id = id(),
        params = params(),
        key = key(),
        stack = stack
    ) to navKey
}

fun BackStack.id(): String = when (this) {
    is InternalBackStack -> id
}

fun BackStack.params(): JsonType = when (this) {
    is InternalBackStack -> params
}

fun BackStack.key(): BoxNavKey = when (this) {
    is InternalBackStack -> key
}

fun BackStack.stack(): ZipList<NavKey> = when (this) {
    is InternalBackStack -> stack
}


fun backstack(
    id: String,
    params: JsonType = JsonNull,
    key: BoxNavKey,
    stack: ZipList<NavKey>
): BackStack {
    return InternalBackStack(
        id = id,
        params = params,
        key = key,
        stack = stack
    )
}
