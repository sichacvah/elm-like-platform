package ru.sbermarket.platform.modules

import ru.sbermarket.platform.Dispatch
import ru.sbermarket.platform.Effect
import ru.sbermarket.platform.Meta


typealias Destination = String

typealias DestinationToMsg <Msg> = (url: String) -> Msg

interface Nav {
    fun <Msg> navigate(destination: String, toMsg: (() -> Msg)? = null): Effect<Msg>
}

interface DestinationService {
    fun emit(destination: Destination)
}

class AwaitingDestinationService : DestinationService {
    var lastDestination: Destination? = null
    override fun emit(destination: Destination) {
        lastDestination = destination
    }

}

class DestinationServiceImpl<Msg>(
    val toMsg: DestinationToMsg<Msg>,
    val toBackMsg : () -> Msg,
    val dispatch: Dispatch<Msg>,
    setupBackPressedCallback: (() -> Unit) -> Unit
): DestinationService {

    init {
        setupBackPressedCallback {
            dispatch(toBackMsg())
        }
    }

    override fun emit(destination: Destination) {
        dispatch(toMsg(destination))
    }


}

class NavImpl(
    val provideDestinationService: () -> DestinationService
): Nav {

    override fun <Msg> navigate(destination: Destination, toMsg: (() -> Msg)?): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(id = "NAVIGATE", options = mapOf("destination" to destination)),
            invoke = { dispatch ->
                provideDestinationService().emit(destination)
                val msg = toMsg?.invoke()
                msg?.let { dispatch(it) }
            }
        )
    }
}


