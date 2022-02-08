package ru.sbermarket.platform.modules.navigation

import ru.sbermarket.platform.modules.SelectedTab3
import ru.sbermarket.platform.modules.SelectedTab5
import ru.sbermarket.platform.modules.json.JsonNull
import ru.sbermarket.platform.modules.json.JsonType


// TODO: Think how to create abstractions for those tabs implementations
data class ThreeTabs internal constructor(
    override val id: String,
    override val params: JsonType = JsonNull,
    override val key: NavKey,
    val selectedTab: SelectedTab3,
    val tab1: BackStack,
    val tab2: BackStack,
    val tab3: BackStack,
): Destination {
    sealed interface PopResult {
        object None : PopResult
        data class TabChanged(val selected: SelectedTab3) : PopResult
        data class PopBackStack(val navKey: NavKey) : PopResult
    }
}


fun SelectedTab3.getPrev(): SelectedTab3? = when (this) {
    SelectedTab3.FIRST -> null
    SelectedTab3.SECOND -> SelectedTab3.FIRST
    SelectedTab3.THIRD -> SelectedTab3.SECOND
}


fun ThreeTabs.navKeys(): Set<NavKey> {
    return tab1.stack().asList().asSequence()
        .plus(tab2.stack().asList())
        .plus(tab3.stack().asList())
        .toSet()
}

fun ThreeTabs.selectTab(tab: SelectedTab3): ThreeTabs {
    return copy(selectedTab = tab)
}

fun ThreeTabs.pop(): Pair<ThreeTabs, ThreeTabs.PopResult> {
    val selected = selectedTab
    val selectedTab = selectedTab()
    val (nextTab, destination) = selectedTab.pop()
    // Если в текущем табе можно сделать .pop то возвращаем табы с выбранным табом и с уменьшеным бекстеком в нем
    return destination?.let { dest ->
        mapSelected { nextTab } to ThreeTabs.PopResult.PopBackStack(dest)
    } ?: run {
        // Получаем предыдущий таб в другом случае и сетим его
        val prevSelected = selected.getPrev()
        prevSelected?.let { selectedTab ->
            selectTab(prevSelected) to ThreeTabs.PopResult.TabChanged(selectedTab)
        } ?: this@pop to ThreeTabs.PopResult.None
        // Если выбранный таб номер 1 и в нем один экран ничего не делаем
    }
}

fun ThreeTabs.push(destination: Destination): ThreeTabs {
    return mapSelected { it.push(destination) }
}

fun ThreeTabs.selectedTab(): BackStack {
    return when (selectedTab) {
        SelectedTab3.FIRST -> tab1
        SelectedTab3.SECOND -> tab2
        SelectedTab3.THIRD -> tab3
    }
}

fun ThreeTabs.mapSelected(mapper: (BackStack) -> BackStack): ThreeTabs {
    return when (selectedTab) {
        SelectedTab3.FIRST -> copy(
            tab1 = mapper(tab1)
        )
        SelectedTab3.SECOND -> copy(
            tab2 = mapper(tab2)
        )
        SelectedTab3.THIRD -> copy(
            tab3 = mapper(tab3)
        )
    }
}

fun ThreeTabs.clear(): ThreeTabs {
    return copy(
        tab1 = tab1.clear(),
        tab2 = tab2.clear(),
        tab3 = tab3.clear(),
        selectedTab = SelectedTab3.FIRST
    )
}


/**
 * FIVE Tabs abstraction
 */

data class FiveTabs internal constructor(
    override val id: String,
    override val params: JsonType = JsonNull,
    override val key: NavKey,
    val selectedTab: SelectedTab5,
    val tab1: BackStack,
    val tab2: BackStack,
    val tab3: BackStack,
    val tab4: BackStack,
    val tab5: BackStack
): Destination {
    sealed interface PopResult {
        object None : PopResult
        data class TabChanged(val selected: SelectedTab5) : PopResult
        data class PopBackStack(val key: NavKey) : PopResult
    }
}

fun FiveTabs.selectTab(tab: SelectedTab5): FiveTabs {
    return copy(selectedTab = tab)
}

fun FiveTabs.selectedTab(): BackStack {
    return when (selectedTab) {
        SelectedTab5.FIRST -> tab1
        SelectedTab5.SECOND -> tab2
        SelectedTab5.THIRD -> tab3
        SelectedTab5.FOURTH -> tab4
        SelectedTab5.FIFTH -> tab5
    }
}

fun FiveTabs.clear(): FiveTabs {
    return copy(
        tab1 = tab1.clear(),
        tab2 = tab2.clear(),
        tab3 = tab3.clear(),
        tab4 = tab4.clear(),
        tab5 = tab5.clear(),
        selectedTab = SelectedTab5.FIRST
    )
}

fun FiveTabs.pop(): Pair<FiveTabs, FiveTabs.PopResult> {
    val selected = selectedTab
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
        } ?: this@pop to FiveTabs.PopResult.None
        // Если выбранный таб номер 1 и в нем один экран ничего не делаем
    }
}

fun FiveTabs.mapSelected(mapper: (BackStack) -> BackStack): FiveTabs {
    return when (selectedTab) {
        SelectedTab5.FIRST -> copy(
            tab1 = mapper(tab1)
        )
        SelectedTab5.SECOND -> copy(
            tab2 = mapper(tab2)
        )
        SelectedTab5.THIRD -> copy(
            tab3 = mapper(tab3)
        )
        SelectedTab5.FOURTH -> copy(
            tab4 = mapper(tab4)
        )
        SelectedTab5.FIFTH -> copy(
            tab5 = mapper(tab5)
        )
    }
}

fun FiveTabs.push(destination: Destination): FiveTabs {
    return mapSelected { it.push(destination) }
}

fun SelectedTab5.getPrev(): SelectedTab5? = when (this) {
    SelectedTab5.FIRST -> null
    SelectedTab5.SECOND -> SelectedTab5.FIRST
    SelectedTab5.THIRD -> SelectedTab5.SECOND
    SelectedTab5.FOURTH -> SelectedTab5.THIRD
    SelectedTab5.FIFTH -> SelectedTab5.FOURTH
}

fun FiveTabs.navKeys(): Set<NavKey> {
    return tab1.stack().asList()
        .asSequence()
        .plus(tab2.stack().asList())
        .plus(tab3.stack().asList())
        .plus(tab4.stack().asList())
        .plus(tab5.stack().asList())
        .toSet()
}