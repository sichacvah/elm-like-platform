package ru.sbermarket.sbermarketlite

import android.app.Application
import ru.sbermarket.platform.PlatformDependencies
import ru.sbermarket.platform.PlatformImpl
import ru.sbermarket.platform.PlatformWithInit
import ru.sbermarket.sbermarketlite.application.core.DataLocalStorage
import ru.sbermarket.sbermarketlite.application.core.HttpKtorClient


class MainApp : Application() {

    companion object {
        var platform: PlatformWithInit? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val context = applicationContext
        platform = PlatformImpl(
            object : PlatformDependencies {
                override val http by lazy { HttpKtorClient() }
                override val local by lazy {
                    DataLocalStorage(
                        context = context
                    )
                }
            }
        )
    }
}

// val webglKey = "82de0e80-2b53-4d04-b5e8-065a25ccab00"