package ru.sbermarket.platform.modules.navigation

import ru.sbermarket.platform.modules.json.JsonNull
import ru.sbermarket.platform.modules.json.JsonType

// SCREEN

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