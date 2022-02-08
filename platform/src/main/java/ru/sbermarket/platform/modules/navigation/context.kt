package ru.sbermarket.platform.modules.navigation


data class DestinationContext(
    val destinationsMap: Map<NavKey, Destination> = mapOf(),
    val rootStack: StackNavKey = StackNavKey()
)

fun DestinationContext.put(pair: Pair<NavKey, Destination>): DestinationContext {
    return copy(
        destinationsMap = destinationsMap.plus(pair)
    )
}

fun DestinationContext.put(map: Map<NavKey, Destination>): DestinationContext {
    return copy(
        destinationsMap = destinationsMap.plus(map)
    )
}

fun DestinationContext.intersection(other: DestinationContext): Set<NavKey> {
    return destinationsMap.keys.intersect(other.destinationsMap.keys)
}

fun DestinationContext.removed(other: DestinationContext): Set<NavKey> {
    return destinationsMap.keys.filter { !other.destinationsMap.keys.contains(it) }.toSet()
}

fun DestinationContext.added(other: DestinationContext): Set<NavKey> {
    return other.destinationsMap.keys.filter { !destinationsMap.keys.contains(it) }.toSet()
}

fun DestinationContext.remove(vararg key: NavKey): DestinationContext {
    return remove(key.toSet())
}

fun DestinationContext.remove(keys: Set<NavKey>): DestinationContext {
    return copy(
        destinationsMap = destinationsMap.minus(keys)
    )
}

fun DestinationContext.get(key: StackNavKey): BackStack? {
    return destinationsMap[key] as BackStack?
}

fun DestinationContext.get(key: ScreenNavKey): Screen? {
    return destinationsMap[key] as Screen?
}

fun DestinationContext.get(key: TabNavKey3): ThreeTabs? {
    return destinationsMap[key] as ThreeTabs?
}

fun DestinationContext.get(key: TabNavKey5): FiveTabs? {
    return destinationsMap[key] as FiveTabs?
}





