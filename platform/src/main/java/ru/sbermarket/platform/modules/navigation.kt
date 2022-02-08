package ru.sbermarket.platform.modules

import ru.sbermarket.platform.Dispatch
import ru.sbermarket.platform.Effect
import ru.sbermarket.platform.Meta
import ru.sbermarket.platform.modules.navigation.*


enum class SelectedTab3 {
    FIRST,
    SECOND,
    THIRD,
}

enum class SelectedTab5 {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    FIFTH
}

interface Nav {
    fun <Msg> push(key: NavKey, destination: Destination, vararg rest: Destination): Effect<Msg>
    fun <Msg> pop(key: NavKey): Effect<Msg>
    fun <Msg> pop(key: NavKey, count: Int): Effect<Msg>
    fun <Msg> clear(key: NavKey): Effect<Msg>

    // Make impossible selection impossible with custom TabNavKeyN and SelectedTabN
    fun <Msg> selectTab(key: TabNavKey3, tab: SelectedTab3): Effect<Msg>
    fun <Msg> selectTab(key: TabNavKey5, tab: SelectedTab5): Effect<Msg>
}

fun DestinationContext.clear(
    key: NavKey
): DestinationContext {
    return when (key) {
        is ScreenNavKey -> clear(key.parent)
        is StackNavKey -> {
            val stack = get(key)
            stack?.let { put(it.key to it.clear()) } ?: this
        }
        is TabNavKey3 -> {
            val tabs3 = get(key)
            tabs3?.let { put(it.key to it.clear()) } ?: this
        }
        is TabNavKey5 -> {
            val tabs5 = get(key)
            tabs5?.let { put(it.key to it.clear()) } ?: this
        }
    }
}

tailrec fun DestinationContext.pop(
    key: NavKey,
    count: Int
): DestinationContext {
    return if (count <= 0) {
        this
    } else {
        when (key) {
            is ScreenNavKey -> pop(key.parent, count)
            is StackNavKey -> {
                val stack = this.get(key)
                val ctx = stack?.let {
                    val (next, navKey) = it.pop()
                    val navKeys = navKey?.let { setOf(it) } ?: setOf()
                    this.remove(navKeys).put(next.key to next)
                } ?: this
                ctx.pop(key, count - 1)
            }
            is TabNavKey3 -> {
                val tabs = get(key)
                if (tabs == null) {
                    this
                } else {
                    val (next, result) = tabs.pop()
                    when (result) {
                        is ThreeTabs.PopResult.None -> this
                        is ThreeTabs.PopResult.TabChanged -> {
                            this.put(next.key to next).pop(key, count - 1)
                        }
                        is ThreeTabs.PopResult.PopBackStack -> {
                            this.put(next.key to next).pop(key, count - 1)
                        }
                    }
                }

            }
            is TabNavKey5 -> {
                val fiveTabs = get(key)
                if (fiveTabs == null) {
                    this
                } else {
                    val (next, result) = fiveTabs.pop()
                    when (result) {
                        is FiveTabs.PopResult.None -> this
                        is FiveTabs.PopResult.TabChanged -> {
                            this.put(next.key to next).pop(key, count - 1)
                        }
                        is FiveTabs.PopResult.PopBackStack -> {
                            this.put(next.key to next).pop(key, count - 1)
                        }
                    }
                }
            }

        }
    }
}

tailrec fun DestinationContext.push(key: NavKey, destinations: List<Destination>): DestinationContext {
    return if (destinations.isEmpty()) {
        this
    } else {
        val dest = destinations.first()
        when (key) {
            is ScreenNavKey -> push(key.parent, destinations)
            is StackNavKey -> {
                val stack = get(key)
                if (stack == null) {
                    this
                } else {
                    val nextStack = stack.push(dest)
                    this.put(nextStack.key to nextStack).put(dest.key to dest).push(key, destinations.drop(1))
                }
            }
            is TabNavKey3 -> {
                val tabs = get(key)
                if (tabs == null) {
                    this
                } else {
                    val nextTabs = tabs.push(dest)
                    this.put(nextTabs.key to nextTabs).put(dest.key to dest).push(key, destinations.drop(1))
                }
            }
            is TabNavKey5 -> {
                val tabs = get(key)
                if (tabs == null) {
                    this
                } else {
                    val nextTabs = tabs.push(dest)
                    this.put(nextTabs.key to nextTabs).put(dest.key to dest).push(key, destinations.drop(1))
                }
            }
        }
    }
}

fun DestinationContext.getDroppedKeys(prev: DestinationContext?): Set<NavKey> {
    return prev?.let {
        prev.removed(it)
    } ?: setOf()
}


fun DestinationContext.getDroppedKeys(navKey: BoxNavKey, prev: DestinationContext): Set<NavKey> {
    return when (navKey) {
        is StackNavKey -> {
            val nextKeys = get(navKey)?.stack()?.asList()?.toSet() ?: setOf()
            val prevKeys = prev.get(navKey)?.stack()?.asList()?.toSet() ?: setOf()
            prevKeys.filter { !nextKeys.contains(it) }.toSet()
        }
        is TabNavKey3 -> {
            val nextKeys = get(navKey)?.navKeys() ?: setOf()
            val prevKeys = prev.get(navKey)?.navKeys() ?: setOf()
            prevKeys.filter { !nextKeys.contains(it) }.toSet()
        }
        is TabNavKey5 -> {
            val nextKeys = get(navKey)?.navKeys() ?: setOf()
            val prevKeys = prev.get(navKey)?.navKeys() ?: setOf()
            prevKeys.filter { !nextKeys.contains(it) }.toSet()
        }
    }
}

fun DestinationContext.selectTab(key: TabNavKey3, tab: SelectedTab3): DestinationContext {
    val tab3 = get(key)
    return tab3?.let {
        val next = it.selectTab(tab)
        this.put(next.key to next)
    } ?: this
}


fun DestinationContext.selectTab(key: TabNavKey5, tab: SelectedTab5): DestinationContext {
    val tab5 = get(key)
    return tab5?.let {
        val next = it.selectTab(tab)
        this.put(next.key to next)
    } ?: this
}

fun DestinationContext.back(key: BoxNavKey = this.rootStack): DestinationContext {
    val last = when (key) {
        is StackNavKey -> get(key)?.stack()?.last()
        is TabNavKey5 -> get(key)?.selectedTab()?.stack()?.last()
        is TabNavKey3 -> get(key)?.selectedTab()?.stack()?.last()
    }

    return when (last) {
        null -> this
        is ScreenNavKey -> this.pop(key, 1)
        is BoxNavKey -> this.back(last)
    }
}


class NavImpl(
    initialDestinationContext: DestinationContext = DestinationContext(),
    var onChange : (next: DestinationContext, prev: DestinationContext) -> Any? = { next, prev -> null },
    var onLastBack: () -> Unit = {}
): Nav {

    var destinationContext = initialDestinationContext

    private fun <Msg> invokeDispatch(dispatch: Dispatch<Msg>, next: DestinationContext) {
        val msg = onChange(next, destinationContext)
        msg?.let {
            dispatch(it as Msg)
        }
        destinationContext = next
    }

    fun <Msg> back(): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(
                id = "Nav.back",
                options = mapOf()
            ),
            invoke = { dispatch ->
                val nextDestinationContext = destinationContext.back()
                if (nextDestinationContext == destinationContext) {
                    onLastBack()
                } else {
                    invokeDispatch(dispatch, nextDestinationContext)
                }
            }
        )
    }

    override fun <Msg> push(key: NavKey, destination: Destination, vararg rest: Destination): Effect<Msg> {
        val destinations = listOf(destination).plus(rest)
        return Effect.ManagedEffect(
            meta = Meta(
                id = "Nav.push",
                options = mapOf(
                    "destinations" to destinations,
                    "key" to key
                )
            ),
            invoke = { dispatch ->
                val nextDestinationContext = destinationContext.push(key, destinations)
                invokeDispatch(dispatch, nextDestinationContext)
            }
        )

    }

    override fun <Msg> pop(key: NavKey): Effect<Msg> {
        return pop(key, 1)
    }

    override fun <Msg> pop(key: NavKey, count: Int): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(
                id = "Nav.pop",
                options = mapOf(
                    "count" to count
                )
            ),
            invoke = { dispatch ->
                val nextDestinationContext = destinationContext.pop(key, count)
                invokeDispatch(dispatch, nextDestinationContext)
            }
        )
    }

    override fun <Msg> clear(key: NavKey): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(
                id = "Nav.clear",
                options = mapOf(
                    "key" to key
                )
            ),
            invoke = { dispatch ->
                val nextDestinationContext = destinationContext.clear(key)
                invokeDispatch(dispatch, nextDestinationContext)
            }
        )
    }

    override fun <Msg> selectTab(key: TabNavKey3, tab: SelectedTab3): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(
                id = "Nav.selectTab",
                options = mapOf(
                    "key" to key,
                    "tab" to tab
                )
            ),
            invoke = { dispatch ->
                val nextDestinationContext = destinationContext.selectTab(key, tab)
                invokeDispatch(dispatch, nextDestinationContext)
            }
        )
    }

    override fun <Msg> selectTab(key: TabNavKey5, tab: SelectedTab5): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(
                id = "Nav.selectTab",
                options = mapOf(
                    "key" to key,
                    "tab" to tab
                )
            ),
            invoke = { dispatch ->
                val nextDestinationContext = destinationContext.selectTab(key, tab)
                invokeDispatch(dispatch, nextDestinationContext)
            }
        )
    }

}



