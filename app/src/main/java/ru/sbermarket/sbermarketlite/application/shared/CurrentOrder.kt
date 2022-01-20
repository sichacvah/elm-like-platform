package ru.sbermarket.sbermarketlite.application.shared

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import ru.sbermarket.platform.*
import ru.sbermarket.platform.modules.json.Decoder
import ru.sbermarket.platform.modules.json.Json
import ru.sbermarket.platform.modules.Http.HttpTaskParams
import ru.sbermarket.platform.modules.Http.Error as HttpError
import ru.sbermarket.platform.modules.Http.Method
import ru.sbermarket.platform.modules.json.Json.Decode.convert
import ru.sbermarket.platform.modules.json.Json.Decode.field
import ru.sbermarket.platform.modules.json.Json.Decode.string

interface CurrentOrderFeature {
    fun update(config: Config, msg: CurrentOrder.Msg, model: CurrentOrder.Model): Pair<CurrentOrder.Model, Effect<CurrentOrder.Msg>>
    fun init(config: Config): Pair<CurrentOrder.Model, Effect<CurrentOrder.Msg>>
}


data class Order(
    val number: String
) {
    companion object {
        fun decoder(): Decoder<Order> {
            return convert(
                field("number", string()),
                ::Order
            )
        }

        fun responseDecoder(): Decoder<Order> {
            return field("order", decoder())
        }
    }
}

fun CurrentOrder.Model.getNumber(): String? {
    return when (this) {
        is CurrentOrder.Model.Loading -> number
        is CurrentOrder.Model.OrderLoaded -> order.number
        is CurrentOrder.Model.OrderLoadingError -> number
        is CurrentOrder.Model.NotInitialized -> null
    }

}

fun CurrentOrder.Model.prepareToEncode(): CurrentOrder.Model {
    return when (this) {
        is CurrentOrder.Model.Loading -> CurrentOrder.Model.NotInitialized
        is CurrentOrder.Model.OrderLoadingError -> CurrentOrder.Model.NotInitialized
        is CurrentOrder.Model.OrderLoaded -> this
        is CurrentOrder.Model.NotInitialized -> this
    }
}

fun CurrentOrder.Model.error(error: String): CurrentOrder.Model = when (this) {
    is CurrentOrder.Model.OrderLoaded -> this
    else -> CurrentOrder.Model.OrderLoadingError(error)
}

@Composable
fun NotInitializedView() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "NotInitialized", modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
    }
}

@Composable
fun OrderLoadingError(error: String) {
    Log.e("ERROR", error)
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = error, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
    }
}

@Composable
fun OrderLoading() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun Order(order: Order) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "OrderLoaded - number: ${order.number}", modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
    }
}


object CurrentOrder  {

    @Composable
    fun View(model: Model) {
        when (model) {
            is Model.NotInitialized -> NotInitializedView()
            is Model.OrderLoadingError -> OrderLoadingError(error = model.error)
            is Model.Loading -> OrderLoading()
            is Model.OrderLoaded -> Order(model.order)
        }
    }

    sealed class Msg {
        object NoOp : Msg()
        data class OrderNumberLoaded(
            val orderNumber: String?
        ): Msg()
        data class OrderLoaded(
            val result: Result<HttpError, Order>
        ): Msg()
    }

    sealed class Model {
        object NotInitialized : Model()
        data class OrderLoadingError(
            val error: String,
            val number: String? = null
        ): Model()

        data class OrderLoaded(
            val order: Order
        ): Model()
        data class Loading(
            val number: String? = null
        ) : Model()
    }

    private fun Platform.loadOrderByNumber(config: Config, number: String): Effect<Msg> {
        return Http.request(
            HttpTaskParams(
                method = Method.GET,
                url = "${config.baseUrl}/v2/orders/${number}",
                expect = Http.expectJson(
                    decoder = Order.responseDecoder(),
                    toMsg = Msg::OrderLoaded
                )
            )
        )
    }

    private fun Platform.loadOrCreate(config: Config): Effect<Msg> {
        return Http.request(HttpTaskParams(
            method = Method.POST,
            url = "${config.baseUrl}/v2/orders",
            expect = Http.expectJson(
                decoder = Order.responseDecoder(),
                toMsg = Msg::OrderLoaded
            )
        ))
    }

    private fun Platform.loadOrder(config: Config, number: String?): Effect<Msg> {
        return number?.let { n -> loadOrderByNumber(config, n) } ?: loadOrCreate(config)
    }

    private const val ORDER_NUMBER_KEY = "orderNumber"

    private fun Platform.loadNumberEffect(): Effect<Msg> {
        return Storage.getItem(
            ORDER_NUMBER_KEY,
            toMsg = Msg::OrderNumberLoaded
        )
    }

    private fun Platform.numberLoaded(config: Config, number: String?, model: Model): Pair<Model, Effect<Msg>> {
        return when (model) {
            is Model.OrderLoaded -> model to none()
            is Model.Loading -> model.copy(number = number) to loadOrder(config, number)
            is Model.OrderLoadingError -> Model.Loading(number = number) to loadOrder(config, number)
            is Model.NotInitialized -> Model.Loading(number = number) to loadOrder(config, number)
        }
    }

    private fun Platform.setNumber(number: String): Effect<Msg> {
        return Storage.setItem(
            key = ORDER_NUMBER_KEY,
            value = number,
            toMsg = { Msg.NoOp }
        )
    }

    private fun Platform.orderLoaded(config: Config, result: Result<HttpError, Order>, model: Model): Pair<Model, Effect<Msg>> {
        return when (result) {
            is Result.Success -> Model.OrderLoaded(result.result) to setNumber(result.result.number)
            is Result.Error -> model.error(result.error.toString()) to none()
        }
    }


    val provideFeature = featureHolder<CurrentOrderFeature> { platform ->
        object : CurrentOrderFeature {
            override fun update(config: Config, msg: Msg, model: Model): Pair<Model, Effect<Msg>> {
                return when (msg) {
                    is Msg.OrderNumberLoaded -> platform.numberLoaded(config, msg.orderNumber, model)
                    is Msg.OrderLoaded -> platform.orderLoaded(config, msg.result, model)
                    is Msg.NoOp -> model to none()
                }
            }

            override fun init(config: Config): Pair<Model, Effect<Msg>> {
                return Model.Loading() to platform.loadNumberEffect()
            }
        }
    }

}
