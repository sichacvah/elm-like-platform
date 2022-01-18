package ru.sbermarket.sbermarketlite.application.shared

import ru.sbermarket.platform.Effect
import ru.sbermarket.platform.Platform

//
//data class Store(
//    val id : Int,
//    val name : String
//) {
//
//}
//
//
//
//object CurrentStore {
//    sealed class Msg {
//        data class IdLoad(val result: Result<Exception, Int?>): Msg()
//        data class StoreLoaded(val result: Result<Exception, Store>): Msg()
//    }
//    sealed class Model {
//        object NotSelected : Model()
//        object IdLoading : Model()
//        data class StoreLoaded(val store: Store): Model()
//        data class StoreLoading(val id : Int) : Model()
//        data class LoadingError(val error : String, val id: Int) : Model()
//    }
//
//    private const val STORE_ID_KEY = "STORE_KEY_ID"
//
//    private fun Platform.loadStore(config: Config, id: Int): Effect<Msg> {
//        return Http.request(
//            ru.sbermarket.platform.modules.Http.HttpTaskParams(
//                url = "${config.baseUrl}/v2/stores/$id",
//                method = "get",
//                expect = Http.expectJson(
//                    decoder = Store.decode,
//                    toMsg = Msg::StoreLoaded
//                )
//            )
//        )
//    }
//
//    private fun Platform.loadStoreId(): Effect<Msg> {
//        return Storage.getItem(
//            key = STORE_ID_KEY,
//            toMsg = { stringResult ->
//                val result = stringResult.map { string ->
//                    string?.toInt()
//                }
//                Msg.IdLoad(result = result)
//            }
//        )
//    }
//
//    interface Feature {
//        fun init(config: Config): Pair<Model, Effect<Msg>>
//        fun update(
//            config: Config,
//            msg: Msg,
//            model: Model
//        ): Pair<Model, Effect<Msg>>
//    }
//
//    private fun Platform.idLoaded(
//        config: Config,
//        model: Model,
//        result: Result<Exception, Int?>
//    ): Pair<Model, Effect<Msg>> {
//        return when (model) {
//            is Model.StoreLoaded -> model to none()
//            else -> {
//                when (result) {
//                    is Result.Error -> Model.NotSelected to none()
//                    is Result.Success -> {
//                        result.result?.let { id ->
//                            Model.StoreLoading(id) to loadStore(config, id)
//                        } ?: Model.NotSelected to none()
//                    }
//                }
//            }
//        }
//    }
//
//    fun Model.handleError(error: Exception): Model = when (this) {
//        is Model.StoreLoaded -> this
//        is Model.NotSelected -> this
//        is Model.IdLoading -> Model.NotSelected
//        is Model.StoreLoading -> Model.LoadingError(
//            id = id,
//            error = error.message ?: error.toString()
//        )
//        is Model.LoadingError ->  Model.LoadingError(
//            id = id,
//            error = error.message ?: error.toString()
//        )
//    }
//
//
//    val provideFeature = featureHolder<Feature> { platform ->
//        object : Feature {
//            override fun update(
//                config: Config,
//                msg: Msg,
//                model: Model
//            ): Pair<Model, Effect<Msg>> {
//                return when (msg) {
//                    is Msg.IdLoad -> platform.idLoaded(config, model, msg.result)
//                    is Msg.StoreLoaded -> {
//                        when (msg.result) {
//                            is Result.Error -> model.handleError(msg.result.error) to none()
//                            is Result.Success -> Model.StoreLoaded(msg.result.result) to none()
//                        }
//                    }
//                }
//            }
//
//            override fun init(config: Config): Pair<Model, Effect<Msg>> {
//                return Model.IdLoading to platform.loadStoreId()
//            }
//
//        }
//    }
//
//}