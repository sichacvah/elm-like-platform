package ru.sbermarket.platform.modules

import ru.sbermarket.platform.Dispatch
import ru.sbermarket.platform.Effect
import ru.sbermarket.platform.Meta

/**
 * TODO: Use some URL implementation instead of [String]
 */
data class Destination(
    val urlString: String
)


typealias DestinationToMsg <Msg> = (Destination) -> Msg

interface Nav {
    fun <Msg> navigate(destination: Destination, toMsg: (() -> Msg)? = null): Effect<Msg>
}

interface DestinationService {
    fun emit(destination: Destination)
}

class AwaitingDestinationService : DestinationService {
    override fun emit(destination: Destination) {

    }
}

class DestinationServiceImpl<Msg>(
    val toMsg: DestinationToMsg<Msg>,
    val dispatch: Dispatch<Msg>
): DestinationService {

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


