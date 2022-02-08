package ru.sbermarket.platform

import ru.sbermarket.platform.modules.*
import ru.sbermarket.platform.modules.HttpImpl
import ru.sbermarket.platform.modules.LocalImpl
import ru.sbermarket.platform.modules.navigation.*


interface Platform {
    val Http: Http
    val Storage: Local
    val Nav: Nav
}

interface PlatformInitializer {
    fun <Model, Msg> init(
        init: Platform.() -> Pair<Model, Effect<Msg>>,
        update: Platform.(Msg, Model)  -> Pair<Model, Effect<Msg>>,
        destinationContext: DestinationContext,
        onNavigationChange: (next: DestinationContext, prev: DestinationContext) -> Msg,
        setupBackPressedCallback: (() -> Unit) -> Unit
    ): FlowRuntime<Model, Msg>
}

interface PlatformWithInit : PlatformInitializer, Platform

class PlatformImpl(
    private val dependencies: PlatformDependencies
): PlatformWithInit {

    override val Http: Http by lazy { HttpImpl(dependencies.http) }
    override val Storage: Local by lazy { LocalImpl(dependencies.local) }
    override val Nav: Nav by lazy { NavImpl() }

    override fun <Model, Msg> init(
        init: Platform.() -> Pair<Model, Effect<Msg>>,
        update: Platform.(Msg, Model) -> Pair<Model, Effect<Msg>>,
        destinationContext: DestinationContext,
        onNavigationChange: (next: DestinationContext, prev: DestinationContext) -> Msg,
        setupBackPressedCallback : (() -> Unit) -> Unit
    ): FlowRuntime<Model, Msg> {

        val runtime = flowRuntime(
            platform = this,
            init = init,
            update = update
        )

        (Nav as NavImpl)
        setupBackPressedCallback {
            (Nav as NavImpl).back<Msg>()
        }
        (Nav as NavImpl).destinationContext = destinationContext
        (Nav as NavImpl).onChange = onNavigationChange

        return runtime
    }
}