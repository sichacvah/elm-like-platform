package ru.sbermarket.sbermarketlite.application

import ru.sbermarket.platform.Platform
import ru.sbermarket.sbermarketlite.application.shared.SharedState


class AppContext(
    private val platform: Platform,
    val sharedState: SharedState.Model
): Platform by platform
