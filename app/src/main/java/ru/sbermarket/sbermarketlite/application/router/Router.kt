package ru.sbermarket.sbermarketlite.application.router

import ru.sbermarket.platform.modules.json.JsonNull
import ru.sbermarket.platform.modules.json.JsonType


sealed interface NavKey
internal data class InternalNavKey(
    val value: String
): NavKey

fun key(string: String): NavKey = InternalNavKey(string)
fun NavKey.plus(other: NavKey): NavKey {
    when (this) {
        is InternalNavKey -> {
            when (other) {
                is InternalNavKey -> {
                    return key(this.value + ":" + other.value)
                }
            }
        }
    }
}

fun BackStack.navKey(id: String): NavKey {
    val parentKey = key()
    return parentKey.plus(key(id))
}

sealed interface Destination {
    val id: String
    val params: JsonType
    val key: NavKey
}

sealed interface Screen: Destination
internal data class ScreenImpl(
    override val id: String,
    override val params: JsonType,
    override val key: NavKey
): Screen

fun screen(id: String, params: JsonType = JsonNull, key: NavKey): Screen {
    return ScreenImpl(
        id,
        params,
        key
    )
}

sealed interface BackStack : Destination

internal data class InternalBackStack(
    val stack: ZipList<Destination>,
    override val id: String,
    override val params: JsonType,
    override val key: NavKey
): BackStack

fun BackStack.top(): Destination {
    return stack().head()
}

fun BackStack.push(destination: Destination): BackStack {
    val stack = stack().push(destination)
    return backstack(
        id = id(),
        params = params(),
        key = key(),
        stack = stack
    )
}

fun BackStack.push(id: String, params: JsonType, key: NavKey? = null): BackStack {
    val finalKey = key ?: navKey(id)
    val destination = screen(id, params, finalKey)
    return push(destination = destination)
}

fun BackStack.pop(): Pair<BackStack, Destination?> {
    val (stack, destination) = stack().pop()
    return backstack(
        id = id(),
        params = params(),
        key = key(),
        stack = stack
    ) to destination
}

fun BackStack.id(): String = when (this) {
    is InternalBackStack -> id
}

fun BackStack.params(): JsonType = when (this) {
    is InternalBackStack -> params
}

fun BackStack.key(): NavKey = when (this) {
    is InternalBackStack -> key
}

fun BackStack.stack(): ZipList<Destination> = when (this) {
    is InternalBackStack -> stack
}

fun backstack(
    id: String,
    params: JsonType = JsonNull,
    key: NavKey,
    destination: Destination
): BackStack {
    return backstack(
        id = id,
        params = params,
        key = key,
        stack = ziplist(destination)
    )
}

fun backstack(
    id: String,
    params: JsonType = JsonNull,
    key: NavKey,
    stack: ZipList<Destination>
): BackStack {
    return InternalBackStack(
        id = id,
        params = params,
        key = key,
        stack = stack
    )
}

sealed interface FiveTabs: Destination {
    enum class SelectedTab {
        FIRST,
        SECOND,
        THIRD,
        FOURTH,
        FIFTH
    }

    sealed class PopResult {
        object Nothing : PopResult()
        data class TabChanged(val selectedTab: SelectedTab) : PopResult()
        data class PopBackStack(val destination: Destination) : PopResult()
    }
}

fun FiveTabs.SelectedTab.getPrev(): FiveTabs.SelectedTab? {
    return when (this) {
        FiveTabs.SelectedTab.FIRST -> null
        FiveTabs.SelectedTab.SECOND -> FiveTabs.SelectedTab.FIRST
        FiveTabs.SelectedTab.THIRD -> FiveTabs.SelectedTab.SECOND
        FiveTabs.SelectedTab.FOURTH -> FiveTabs.SelectedTab.THIRD
        FiveTabs.SelectedTab.FIFTH -> FiveTabs.SelectedTab.FOURTH
    }
}

internal data class FiveTabsImpl(
    val tab1: BackStack,
    val tab2: BackStack,
    val tab3: BackStack,
    val tab4: BackStack,
    val tab5: BackStack,
    val selected: FiveTabs.SelectedTab,
    override val id : String,
    override val params : JsonType,
    override val key: NavKey
): FiveTabs

fun FiveTabs.push(destination: Destination): FiveTabs {
    return mapSelected {
        it.push(destination)
    }
}

fun FiveTabs.pop(): Pair<FiveTabs, FiveTabs.PopResult> {
    val selected = selected()
    val selectedTab = selectedTab()
    val (nextTab, destination) = selectedTab.pop()
    // Если в текущем табе можно сделать .pop то возвращаем табы с выбранным табом и с уменьшеным бекстеком в нем
    return destination?.let { dest ->
        mapSelected { nextTab } to FiveTabs.PopResult.PopBackStack(dest)
    } ?: run {
        // Получаем предыдущий таб в другом случае и сетим его
        val prevSelected = selected.getPrev()
        prevSelected?.let { selectedTab ->
            selectTab(prevSelected) to FiveTabs.PopResult.TabChanged(selectedTab)
        } ?: this@pop to FiveTabs.PopResult.Nothing
        // Если выбранный таб номер 1 и в нем один экран ничего не делаем
    }
}

fun FiveTabs.mapSelected(t: (BackStack) -> BackStack): FiveTabs {
    val builder = fiveTabsBuilder(id = id(), key = key(), params = params())
    return when (selected()) {

        FiveTabs.SelectedTab.FIRST -> {
            builder(
                t(first()),
                second(),
                third(),
                fourth(),
                fifth(),
                selected()
            )
        }
        FiveTabs.SelectedTab.SECOND -> {
            builder(
                first(),
                t(second()),
                third(),
                fourth(),
                fifth(),
                selected()
            )
        }
        FiveTabs.SelectedTab.THIRD -> {
            builder(
                first(),
                second(),
                t(third()),
                fourth(),
                fifth(),
                selected()
            )
        }
        FiveTabs.SelectedTab.FOURTH -> {
            builder(
                first(),
                second(),
                third(),
                t(fourth()),
                fifth(),
                selected()
            )
        }
        FiveTabs.SelectedTab.FIFTH -> builder(
            first(),
            second(),
            third(),
            fourth(),
            t(fifth()),
            selected()
        )
    }
}


fun FiveTabs.selectedTab(): BackStack {
    return when (selected()) {
        FiveTabs.SelectedTab.FIRST -> first()
        FiveTabs.SelectedTab.SECOND -> second()
        FiveTabs.SelectedTab.THIRD -> third()
        FiveTabs.SelectedTab.FOURTH -> fourth()
        FiveTabs.SelectedTab.FIFTH -> fifth()
    }
}

fun FiveTabs.selected(): FiveTabs.SelectedTab {
    return when (this) {
        is FiveTabsImpl -> {
            selected
        }
    }
}

fun FiveTabs.selectTab(tab: FiveTabs.SelectedTab): FiveTabs {
    val builder = fiveTabsBuilder(id = id(), key = key(), params = params())
    return builder(
        first(),
        second(),
        third(),
        fourth(),
        fifth(),
        tab
    )
}

fun FiveTabs.first(): BackStack {
    return when (this) {
        is FiveTabsImpl -> tab1
    }
}

fun FiveTabs.second(): BackStack {
    return when (this) {
        is FiveTabsImpl -> tab2
    }
}

fun FiveTabs.third(): BackStack {
    return when (this) {
        is FiveTabsImpl -> tab3
    }
}

fun FiveTabs.fourth(): BackStack {
    return when (this) {
        is FiveTabsImpl -> tab4
    }
}

fun FiveTabs.fifth(): BackStack {
    return when (this) {
        is FiveTabsImpl -> tab5
    }
}

fun FiveTabs.id(): String = when (this) {
    is FiveTabsImpl -> id
}

fun FiveTabs.params(): JsonType = when (this) {
    is FiveTabsImpl -> params
}

fun FiveTabs.key(): NavKey = when (this) {
    is FiveTabsImpl -> key
}

typealias FiveTabsBuilder = (
    tab1: BackStack,
    tab2: BackStack,
    tab3: BackStack,
    tab4: BackStack,
    tab5: BackStack,
    selected: FiveTabs.SelectedTab
) -> FiveTabs

fun fiveTabsBuilder(
    id : String,
    params: JsonType = JsonNull,
    key : NavKey
): FiveTabsBuilder {
    return { tab1, tab2, tab3, tab4, tab5, selected ->
        fiveTabs(
            tab1,
            tab2,
            tab3,
            tab4,
            tab5,
            selected,
            id,
            params,
            key
        )
    }
}

fun fiveTabs(
    tab1: BackStack,
    tab2: BackStack,
    tab3: BackStack,
    tab4: BackStack,
    tab5: BackStack,
    selected: FiveTabs.SelectedTab = FiveTabs.SelectedTab.FIRST,
    id : String,
    params: JsonType = JsonNull,
    key : NavKey
): FiveTabs {
    return FiveTabsImpl(
        tab1 = tab1,
        tab2 = tab2,
        tab3 = tab3,
        tab4 = tab4,
        tab5 = tab5,
        selected = selected,
        id = id,
        params = params,
        key = key
    )
}

