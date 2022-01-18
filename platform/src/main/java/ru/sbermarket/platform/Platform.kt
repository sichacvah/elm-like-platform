package ru.sbermarket.platform

import ru.sbermarket.platform.modules.Http
import ru.sbermarket.platform.modules.HttpImpl
import ru.sbermarket.platform.modules.Local
import ru.sbermarket.platform.modules.LocalImpl


interface Platform {
    val Http: Http
    val Storage: Local
}

interface PlatformInitializer {
    fun <Model, Msg> init(
        init: Platform.() -> Pair<Model, Effect<Msg>>,
        update: Platform.(Msg, Model)  -> Pair<Model, Effect<Msg>>
    ): FlowRuntime<Model, Msg>
}

interface PlatformWithInit : PlatformInitializer, Platform

class PlatformImpl(
    dependencies: PlatformDependencies
): PlatformWithInit {

    override val Http: Http by lazy { HttpImpl(dependencies.http) }
    override val Storage: Local by lazy { LocalImpl(dependencies.local) }

    override fun <Model, Msg> init(
        init: Platform.() -> Pair<Model, Effect<Msg>>,
        update: Platform.(Msg, Model) -> Pair<Model, Effect<Msg>>
    ): FlowRuntime<Model, Msg> {
        return flowRuntime(
            platform = this,
            init = init,
            update = update
        )
    }
}