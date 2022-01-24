package ru.sbermarket.sbermarketlite.application

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.sbermarket.platform.*
import ru.sbermarket.platform.modules.Destination
import ru.sbermarket.sbermarketlite.application.features.select_address.*
import ru.sbermarket.sbermarketlite.application.features.select_store.SelectStore
import ru.sbermarket.sbermarketlite.application.router.*
import ru.sbermarket.sbermarketlite.application.shared.Config
import ru.sbermarket.sbermarketlite.application.shared.SharedState
import ru.sbermarket.sbermarketlite.application.shared.status
import ru.sbermarket.sbermarketlite.rememberMapTo

sealed interface Screen {
    data class SelectAddressScreen(val model : SelectAddress.AddressState) : Screen
    data class SelectStoreScreen(val model: SelectStore.Model) : Screen
}



object App {

    @Composable
    fun View(model: Model, dispatch: Dispatch<Msg>) {

        when (model) {
            is Model.Initializing -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
            is Model.Error -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Error...")
                }
            }
            is Model.Initialized -> {
                val screen = model.backstack.top()
                RouterView(shared = model.shared, screen = screen, dispatch = dispatch)
            }
        }
    }

    @Composable
    fun RouterView(shared: SharedState.Model, screen: Screen, dispatch: Dispatch<Msg>) {
        when (screen) {
            is Screen.SelectAddressScreen -> {
                SelectAddress.View(
                    model = screen.model,
                    dispatch = dispatch.mapTo(Msg::GotSelectAddressMsg)
                )
            }
            is Screen.SelectStoreScreen -> {
                SelectStore.View(
                    model = screen.model,
                    dispatch = dispatch.mapTo(Msg::GotSelectStoreMsg)
                )
            }
        }
    }

    sealed class Model {
        data class Initializing(val shared: SharedState.Model) : Model()
        data class Error(val shared: SharedState.Model) : Model()
        data class Initialized(
            val shared: SharedState.Model,
            val backstack: BackStack<Screen>
        ) : Model()

        fun setShared(shared: SharedState.Model): Model {
            return when (this) {
                is Initializing -> copy(shared = shared)
                is Error -> copy(shared = shared)
                is Initialized -> copy(shared = shared)
            }
        }

        fun shared(): SharedState.Model {
            return when (this) {
                is Initializing -> shared
                is Error -> shared
                is Initialized -> shared
            }
        }
    }

    sealed interface Msg {
        data class SharedMsg(
            val subMsg: SharedState.Msg
        ) : Msg
        data class GotSelectAddressMsg(
            val subMsg: SelectAddress.Msg
        ) : Msg
        data class GotSelectStoreMsg(
            val subMsg: SelectStore.Msg
        ) : Msg
        data class DestinationChanged(
            val destination: Destination
        ) : Msg
        object Back : Msg
    }

    fun handlePageMsg(platform: Platform, config: Config, msg: Msg, currentScreen: Screen): Pair<Screen, Effect<Msg>> {
        return when {
            (msg is Msg.GotSelectStoreMsg && currentScreen is Screen.SelectStoreScreen) -> {
                val (model, effect) = SelectStore.update(msg.subMsg, currentScreen.model)
                Screen.SelectStoreScreen(model) to effect.mapTo(Msg::GotSelectStoreMsg)
            }
            (msg is Msg.GotSelectAddressMsg && currentScreen is Screen.SelectAddressScreen) -> {
                val (model, effect) = SelectAddress.update(platform, config, msg.subMsg, currentScreen.model)
                Screen.SelectAddressScreen(model) to effect.mapTo(Msg::GotSelectAddressMsg)
            }
            else -> currentScreen to none()
        }
    }

    fun update(platform: Platform, msg: Msg, model: Model): Pair<Model, Effect<Msg>> {
        return when (msg) {
            is Msg.SharedMsg -> {
                val (shared, sharedEff) = SharedState.provideFeature(platform).update(msg.subMsg, model.shared())
                when (shared.status()) {
                    SharedState.Status.READY -> {
                        val (selectAddress, selectAddressEffect) = SelectAddress.init(
                            platform = platform,
                            config = shared.config
                        )
                        Model.Initialized(shared = shared, backstack = BackStack(current = Screen.SelectAddressScreen(selectAddress))) to batch(
                            selectAddressEffect.mapTo(Msg::GotSelectAddressMsg),
                            sharedEff.mapTo(Msg::SharedMsg)
                        )
                    }
                    else -> model.setShared(shared = shared) to sharedEff.mapTo(Msg::SharedMsg)
                }
            }
            is Msg.Back -> {
                when (model) {
                    is Model.Initialized -> {
                        val (backstack) = model.backstack.pop()
                        model.copy(backstack = backstack) to none()
                    }
                    else -> model to none()
                }
            }
            is Msg.DestinationChanged -> {
                Log.e("DEST", msg.destination)
                when (model) {
                    is Model.Initialized -> {
                        val backstack = model.backstack.push(Screen.SelectStoreScreen(SelectStore.Model.None))
                        model.copy(backstack = backstack) to none()
                    }
                    else -> model to none()
                }
            }
            else -> {
                when (model) {
                    is Model.Initialized -> {
                        val (currentScreen, effect) = handlePageMsg(platform, model.shared.config, msg, model.backstack.top())
                        val backStack = model.backstack.replace(currentScreen)
                        model.copy(backstack = backStack) to effect
                    }
                    is Model.Initializing -> {
                        model to none()
                    }
                    is Model.Error -> {
                        model to none()
                    }
                }
            }
        }
    }

    fun init(platform: Platform): Pair<Model, Effect<Msg>> {
        val sharedStateFeature = SharedState.provideFeature(platform)
        val (shared, sharedEffect) = sharedStateFeature.init()

        return Model.Initializing(shared = shared) to batch(
            sharedEffect.mapTo(Msg::SharedMsg)
        )
    }

}