package ru.sbermarket.sbermarketlite.application.router

import ru.sbermarket.platform.Effect
import ru.sbermarket.platform.batch
import ru.sbermarket.platform.none

sealed interface Navigation<ScreenModel>

sealed interface StackNavigation<ScreenModel> : Navigation<ScreenModel>

internal data class StackNavigationImpl<ScreenModel>(
    internal val screens: Map<NavKey, ScreenModel>,
    val backstack: BackStack
): StackNavigation<ScreenModel>

internal fun <SM> StackNavigation<SM>.mapBackstack(map: (BackStack) -> BackStack): StackNavigation<SM> {
    return when (this) {
        is StackNavigationImpl -> {
            copy(
                backstack = map(backstack)
            )
        }
    }
}

internal fun <SM> StackNavigation<SM>.remove(vararg next: NavKey): StackNavigation<SM> {
    return when (this) {
        is StackNavigationImpl -> {
            copy(
                screens = screens.minus(next.toSet())
            )
        }
    }
}

internal fun <SM> StackNavigation<SM>.addScreens(vararg next: Pair<NavKey, SM>): StackNavigation<SM> {
    return when (this) {
        is StackNavigationImpl -> {
            copy(
                screens = screens.plus(next)
            )
        }
    }
}

internal fun <SM> StackNavigation<SM>.addScreen(navKey: NavKey, model: SM): StackNavigation<SM> {
    return when (this) {
        is StackNavigationImpl -> {
            copy(
                screens = screens.plus(navKey to model)
            )
        }
    }
}

fun StackNavigation<*>.backstack(): BackStack {
    return when (this) {
        is StackNavigationImpl<*> -> backstack
    }
}

internal fun <ScreenModel> Navigation<ScreenModel>.cachedModel(key: NavKey): ScreenModel? = when (this) {
    is StackNavigationImpl<ScreenModel> -> this.screens[key]
    is FiveTabsNavigationImpl<ScreenModel> -> this.screens[key]
}

fun <SM, Msg> StackNavigation<SM>.match(
    matcher: (destination: Destination) -> Pair<SM, Effect<Msg>>
): Pair<SM, Effect<Msg>> {
    val destination = backstack().top()
    val modelInMap = cachedModel(destination.key)
    return modelInMap?.let { it to none() } ?: matcher(destination)
}

fun <SM, Msg> StackNavigation<SM>.push(
    matcher: (destination: Destination) -> Pair<SM, Effect<Msg>>,
    destination: Destination
): Pair<StackNavigation<SM>, Effect<Msg>> {
    val next = mapBackstack {
        it.push(destination)
    }
    val (screen, effect) = next.match(matcher)
    return next.addScreen(destination.key, screen) to effect
}

fun <SM> StackNavigation<SM>.pop(): StackNavigation<SM> {
    val (nextBackStack, dest) = backstack().pop()
    return dest?.let {
        val next = remove(it.key)
        next.mapBackstack { nextBackStack }
    } ?: this
}

const val ROOT_ID = "ROOT"

fun <SM, Msg> navigation(
    destination : Destination,
    matcher: (destination: Destination) -> Pair<SM, Effect<Msg>>
): Pair<StackNavigation<SM>, Effect<Msg>> {
    val destinations = ziplist(destination)
    return navigation(
        destinations = destinations,
        matcher = matcher
    )
}

fun <SM, Msg> navigation(
    destinations : ZipList<Destination>,
    matcher : (destination: Destination) -> Pair<SM, Effect<Msg>>
): Pair<StackNavigation<SM>, Effect<Msg>> {
    return navigation(
        backstack = backstack(id = ROOT_ID, key = key(ROOT_ID), stack = destinations),
        matcher = matcher
    )
}

fun <SM, Msg> navigation(
    backstack : BackStack,
    matcher : (destination: Destination) -> Pair<SM, Effect<Msg>>
): Pair<StackNavigation<SM>, Effect<Msg>> {
    val (screens, effects) = backstack.stack().asList().map { dest ->
        dest.key to matcher(dest)
    }.fold(mapOf<NavKey, SM>() to listOf<Effect<Msg>>()) { (screens, effects), pair ->
        val key = pair.first
        val (screen, effect) = pair.second
        screens.plus(key to screen) to effects.plus(effect)
    }

    return StackNavigationImpl(
        screens = screens,
        backstack = backstack
    ) to batch(effects)
}

/// FIVE_TABS_NAVIGATION


sealed interface FiveTabsNavigation<ScreenModel>: Navigation<ScreenModel>


internal data class FiveTabsNavigationImpl<SM>(
    val screens: Map<NavKey, SM> = mapOf(),
    val fiveTabs: FiveTabs
) : FiveTabsNavigation<SM>


internal fun <SM> FiveTabsNavigation<SM>.mapTabs(mapper: (FiveTabs) -> FiveTabs): FiveTabsNavigation<SM> {
    return when (this) {
        is FiveTabsNavigationImpl -> {
            copy(
                fiveTabs = mapper(fiveTabs)
            )
        }
    }
}

internal fun <SM> FiveTabsNavigation<SM>.remove(vararg next: NavKey): FiveTabsNavigation<SM> {
    return when (this) {
        is FiveTabsNavigationImpl -> {
            copy(
                screens = screens.minus(next.toSet())
            )
        }
    }
}

internal fun <SM> FiveTabsNavigation<SM>.addScreens(vararg next: Pair<NavKey, SM>): FiveTabsNavigation<SM> {
    return when (this) {
        is FiveTabsNavigationImpl -> {
            copy(
                screens = screens.plus(next)
            )
        }
    }
}

internal fun <SM> FiveTabsNavigation<SM>.addScreen(navKey: NavKey, model: SM): FiveTabsNavigation<SM> {
    return when (this) {
        is FiveTabsNavigationImpl -> {
            copy(
                screens = screens.plus(navKey to model)
            )
        }
    }
}

fun FiveTabsNavigation<*>.tabs(): FiveTabs {
    return when (this) {
        is FiveTabsNavigationImpl<*> -> fiveTabs
    }
}


fun <SM, Msg> FiveTabsNavigation<SM>.match(
    matcher: (destination: Destination) -> Pair<SM, Effect<Msg>>
): Pair<SM, Effect<Msg>> {
    val head = tabs().selectedTab().stack().head()
    val modelInMap = cachedModel(head.key)
    return modelInMap?.let { it to none() } ?: matcher(head)
}

fun <SM> FiveTabsNavigation<SM>.pop(): FiveTabsNavigation<SM> {
    val (tabs, result) = tabs().pop()
    return when (result) {
        is FiveTabs.PopResult.Nothing -> this
        is FiveTabs.PopResult.TabChanged -> mapTabs { tabs }
        is FiveTabs.PopResult.PopBackStack -> {
            mapTabs { tabs }.remove(result.destination.key)
        }
    }
}

fun <SM, Msg> FiveTabsNavigation<SM>.push(
    matcher: (destination: Destination) -> Pair<SM, Effect<Msg>>,
    destination: Destination
): Pair<FiveTabsNavigation<SM>, Effect<Msg>> {
    val next = mapTabs { fiveTabs ->
        fiveTabs.push(destination = destination)
    }

    val (screen, effect) = next.match(matcher)
    return next.addScreen(destination.key, screen) to effect
}

fun <SM> FiveTabsNavigation<SM>.selectFirst(): FiveTabsNavigation<SM> {
    return mapTabs { fiveTabs ->
        fiveTabs.selectTab(FiveTabs.SelectedTab.FIRST)
    }
}

fun <SM> FiveTabsNavigation<SM>.selectSecond(): FiveTabsNavigation<SM> {
    return mapTabs { fiveTabs ->
        fiveTabs.selectTab(FiveTabs.SelectedTab.SECOND)
    }
}

fun <SM> FiveTabsNavigation<SM>.selectThird(): FiveTabsNavigation<SM> {
    return mapTabs { fiveTabs ->
        fiveTabs.selectTab(FiveTabs.SelectedTab.THIRD)
    }
}

fun <SM> FiveTabsNavigation<SM>.selectFourth(): FiveTabsNavigation<SM> {
    return mapTabs { fiveTabs ->
        fiveTabs.selectTab(FiveTabs.SelectedTab.FOURTH)
    }
}

fun <SM> FiveTabsNavigation<SM>.selectFifth(): FiveTabsNavigation<SM> {
    return mapTabs { fiveTabs ->
        fiveTabs.selectTab(FiveTabs.SelectedTab.FIFTH)
    }
}

