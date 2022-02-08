package ru.sbermarket.platform.modules.navigation

import ru.sbermarket.platform.modules.json.JsonType

sealed interface Destination {
    val id: String
    val params: JsonType
    val key: NavKey
}