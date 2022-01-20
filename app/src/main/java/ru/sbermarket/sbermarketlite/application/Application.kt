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
import ru.sbermarket.sbermarketlite.application.features.select_address.SelectAddress
import ru.sbermarket.sbermarketlite.application.shared.CurrentOrder
import ru.sbermarket.sbermarketlite.application.shared.SharedState
import ru.sbermarket.sbermarketlite.application.shared.status
import ru.sbermarket.sbermarketlite.rememberMapTo


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
                SelectAddress.View(
                    model = model.selectAddress,
                    dispatch = dispatch.rememberMapTo(Msg::SelectAddressMsg)
                )
            }
        }
    }


    sealed class Model {
        data class Initializing(val shared: SharedState.Model) : Model()
        data class Error(val shared: SharedState.Model) : Model()
        data class Initialized(
            val shared: SharedState.Model,
            val selectAddress: SelectAddress.AddressState
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

    sealed class Msg {
        data class SharedMsg(
            val subMsg: SharedState.Msg
        ) : Msg()
        data class SelectAddressMsg(
            val subMsg: SelectAddress.Msg
        ) : Msg()
        data class DestinationChanged(
            val destination: Destination
        ) : Msg()
    }

    fun update(platform: Platform, msg: Msg, model: Model): Pair<Model, Effect<Msg>> {
        return when (msg) {
            is Msg.DestinationChanged -> {
                Log.e("DESTINATION_CHANGED", msg.destination.urlString)
                model to none()
            }
            is Msg.SharedMsg -> {
                val (shared, sharedEff) = SharedState.provideFeature(platform).update(msg.subMsg, model.shared())
                when (shared.status()) {
                    SharedState.Status.READY -> {
                        val (selectAddress, selectAddressEffect) = SelectAddress.init(
                            platform = platform,
                            config = shared.config
                        )
                        Model.Initialized(shared = shared, selectAddress = selectAddress) to batch(
                            selectAddressEffect.mapTo(Msg::SelectAddressMsg),
                            sharedEff.mapTo(Msg::SharedMsg)
                        )
                    }
                    else -> model.setShared(shared = shared) to sharedEff.mapTo(Msg::SharedMsg)
                }
            }
            is Msg.SelectAddressMsg -> {
                when (model) {
                    is Model.Initialized -> {
                        val (selectAddress, selectAddressEff) = SelectAddress.update(
                            platform = platform,
                            config = model.shared().config,
                            msg = msg.subMsg,
                            model = model.selectAddress
                        )
                        model.copy(selectAddress = selectAddress) to selectAddressEff.mapTo(Msg::SelectAddressMsg)
                    }
                    else -> model to none()
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