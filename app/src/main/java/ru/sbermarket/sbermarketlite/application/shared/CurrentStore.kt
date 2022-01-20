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
//    sealed class AddressState {
//        object NotSelected : AddressState()
//        object IdLoading : AddressState()
//        data class StoreLoaded(val store: Store): AddressState()
//        data class StoreLoading(val id : Int) : AddressState()
//        data class LoadingError(val error : String, val id: Int) : AddressState()
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
//        fun init(config: Config): Pair<AddressState, Effect<Msg>>
//        fun update(
//            config: Config,
//            msg: Msg,
//            model: AddressState
//        ): Pair<AddressState, Effect<Msg>>
//    }
//
//    private fun Platform.idLoaded(
//        config: Config,
//        model: AddressState,
//        result: Result<Exception, Int?>
//    ): Pair<AddressState, Effect<Msg>> {
//        return when (model) {
//            is AddressState.StoreLoaded -> model to none()
//            else -> {
//                when (result) {
//                    is Result.Error -> AddressState.NotSelected to none()
//                    is Result.Success -> {
//                        result.result?.let { id ->
//                            AddressState.StoreLoading(id) to loadStore(config, id)
//                        } ?: AddressState.NotSelected to none()
//                    }
//                }
//            }
//        }
//    }
//
//    fun AddressState.handleError(error: Exception): AddressState = when (this) {
//        is AddressState.StoreLoaded -> this
//        is AddressState.NotSelected -> this
//        is AddressState.IdLoading -> AddressState.NotSelected
//        is AddressState.StoreLoading -> AddressState.LoadingError(
//            id = id,
//            error = error.message ?: error.toString()
//        )
//        is AddressState.LoadingError ->  AddressState.LoadingError(
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
//                model: AddressState
//            ): Pair<AddressState, Effect<Msg>> {
//                return when (msg) {
//                    is Msg.IdLoad -> platform.idLoaded(config, model, msg.result)
//                    is Msg.StoreLoaded -> {
//                        when (msg.result) {
//                            is Result.Error -> model.handleError(msg.result.error) to none()
//                            is Result.Success -> AddressState.StoreLoaded(msg.result.result) to none()
//                        }
//                    }
//                }
//            }
//
//            override fun init(config: Config): Pair<AddressState, Effect<Msg>> {
//                return AddressState.IdLoading to platform.loadStoreId()
//            }
//
//        }
//    }
//
//}