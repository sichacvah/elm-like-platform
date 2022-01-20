package ru.sbermarket.platform

import ru.sbermarket.platform.modules.*
import ru.sbermarket.platform.modules.HttpImpl
import ru.sbermarket.platform.modules.LocalImpl


interface Platform {
    val Http: Http
    val Storage: Local
    val Nav: Nav
}

interface PlatformInitializer {
    fun <Model, Msg> init(
        init: Platform.() -> Pair<Model, Effect<Msg>>,
        update: Platform.(Msg, Model)  -> Pair<Model, Effect<Msg>>,
        onDestinationChange: DestinationToMsg<Msg>
    ): FlowRuntime<Model, Msg>
}

interface PlatformWithInit : PlatformInitializer, Platform

class PlatformImpl(
    dependencies: PlatformDependencies
): PlatformWithInit {

    private var destinationService: DestinationService = AwaitingDestinationService()

    private fun provideDestinationService(): DestinationService {
        return destinationService
    }

    override val Http: Http by lazy { HttpImpl(dependencies.http) }
    override val Storage: Local by lazy { LocalImpl(dependencies.local) }
    override val Nav: Nav by lazy { NavImpl(this::provideDestinationService) }

    override fun <Model, Msg> init(
        init: Platform.() -> Pair<Model, Effect<Msg>>,
        update: Platform.(Msg, Model) -> Pair<Model, Effect<Msg>>,
        onDestinationChange: DestinationToMsg<Msg>
    ): FlowRuntime<Model, Msg> {

        val runtime = flowRuntime(
            platform = this,
            init = init,
            update = update
        )

        destinationService = DestinationServiceImpl(
            toMsg = onDestinationChange,
            dispatch = {
                runtime.dispatch(it)
            }
        )

        return runtime
    }
}