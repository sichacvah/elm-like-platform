package ru.sbermarket.sbermarketlite.application.features.root_stack

import ru.sbermarket.platform.*
import ru.sbermarket.sbermarketlite.application.App
import ru.sbermarket.sbermarketlite.application.AppContext
import ru.sbermarket.sbermarketlite.application.features.select_address.Address
import ru.sbermarket.sbermarketlite.application.features.select_address.SelectAddress
import ru.sbermarket.sbermarketlite.application.features.select_store.SelectStore
import ru.sbermarket.sbermarketlite.application.router.Destination
import ru.sbermarket.sbermarketlite.application.router.StackNavigation
import ru.sbermarket.sbermarketlite.application.router.navigation
import ru.sbermarket.sbermarketlite.application.router.screen
import ru.sbermarket.sbermarketlite.application.shared.SharedState


class Reader<T : Any, U : Any>(val runReader: (T) -> U) {

    companion object {
        fun <T: Any> ask(): Reader<T, T> = Reader { t: T -> t }
    }

    fun <R : Any> local(f: (R) -> T): Reader<R, U> = Reader { r: R ->
        runReader(f(r))
    }
}

//monad
fun <T : Any, Value : Any> Reader.Companion.pure(v: Value): Reader<T, Value> =
    Reader { v }

fun <T : Any, U : Any, R : Any> Reader<T, U>.flatMap(transform: (U) -> Reader<T, R>): Reader<T, R> =
    Reader { t: T -> transform(runReader(t)).runReader(t) }

//functor
fun <T : Any, U : Any, R : Any> Reader<T, U>.map(transform: (U) -> R): Reader<T, R> = Reader { t: T -> transform(runReader(t)) }


typealias AppContextReader<T> = Reader<AppContext, T>

sealed interface Screen
object LoadingScreen : Screen {
    val id = "Splash"
}
data class SelectAddressScreen(val model : SelectAddress.AddressState) : Screen {
    companion object {
        val id = "SelectAddress"
    }
}
data class SelectStoreScreen(val model: SelectStore.Model) : Screen {
    companion object {
        val id = "SelectStore"
    }
}

typealias MatcherReader = AppContextReader<Pair<Screen, Effect<App.Msg>>>
fun matcher(destination: Destination): MatcherReader = Reader { appContext ->
    when (destination.id) {
        LoadingScreen.id -> LoadingScreen to none()
        SelectAddressScreen.id -> {
            val address = Address.decoder().decode(destination.params).toNullable()
            val (model, effect) = SelectAddress.init(appContext, appContext.sharedState.config, address)
            SelectAddressScreen(model) to effect.mapTo(
                App.Msg::GotSelectAddressMsg)
        }
        SelectStoreScreen.id -> {
            SelectStoreScreen(SelectStore.Model.None) to none()
        }
        else -> LoadingScreen to none()
    }
}

data class Model(
    val stack: StackNavigation<Screen>
)

sealed interface Msg

typealias RootStackReader = AppContextReader<Pair<Model, Effect<Msg>>>

object RootStack {
    fun init(): RootStackReader = Reader { appContext ->
        val (stack, effect) = navigation(screen(id = LoadingScreen.id)) {
            matcher(it).runReader(appContext)
        }
        Model(stack = stack) to effect.mapTo {  }
    }
}


