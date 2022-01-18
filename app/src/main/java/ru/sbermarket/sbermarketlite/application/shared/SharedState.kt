package ru.sbermarket.sbermarketlite.application.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.sbermarket.platform.*

data class Config(
    val baseUrl: String = "https://api.sbermarket.ru"
)

typealias SharedStateModel = SharedState.Model
typealias SharedStateMsg = SharedState.Msg

@Composable
fun SharedState.View(model: SharedStateModel, dispatch: Dispatch<SharedStateMsg>) {
    Box(modifier = Modifier.fillMaxSize()) {
        CurrentOrder.View(model.currentOrder)
    }
}



interface SharedStateFeature {
    fun init(): Pair<SharedState.Model, Effect<SharedState.Msg>>
    fun update(
        msg: SharedState.Msg,
        model: SharedState.Model
    ): Pair<SharedState.Model, Effect<SharedState.Msg>>
}

object SharedState {
    sealed class Msg {
        data class ChangeConfig(val config: Config): Msg()
        data class OrderMsg(val subMsg: CurrentOrder.Msg): Msg()
    }


    data class Model(
        val currentOrder: CurrentOrder.Model,
        val config: Config
    )

    val provideFeature = featureHolder<SharedStateFeature> { platform ->
        object : SharedStateFeature {
            override fun init(): Pair<Model, Effect<Msg>> {
                val config = Config()
                val currentOrderFeature = CurrentOrder.provideFeature(platform)
                val (currentOrder, effect) = currentOrderFeature.init(config)

                return Model(
                    currentOrder = currentOrder,
                    config = config
                ) to effect.mapTo(Msg::OrderMsg)
            }

            override fun update(msg: Msg, model: Model): Pair<Model, Effect<Msg>> {
                return when (msg) {
                    is Msg.ChangeConfig -> model.copy(config = msg.config) to none()
                    is Msg.OrderMsg -> {
                        val (currentOrder, effect) = CurrentOrder.provideFeature(platform).update(
                            model.config,
                            msg.subMsg,
                            model.currentOrder
                        )
                        model.copy(
                            currentOrder = currentOrder
                        ) to effect.mapTo(Msg::OrderMsg)
                    }
                }
            }
        }
    }



}

