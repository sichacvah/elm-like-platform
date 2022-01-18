package ru.sbermarket.platform.modules

import ru.sbermarket.platform.*
import ru.sbermarket.platform.Meta
import ru.sbermarket.platform.modules.json.Decoder
import ru.sbermarket.platform.modules.json.Json
import ru.sbermarket.platform.modules.json.JsonType

interface Local {
    sealed class Error {
        class JsonParseError(val message: String): Error()
        class UnexpectedError(val message: String): Error()
    }

    fun <Msg> setItem(
        key: String,
        value: String,
        toMsg: () -> Msg
    ): Effect<Msg>

    fun <Msg> setItemJson(
        key: String,
        value: JsonType,
        toMsg: () -> Msg
    ): Effect<Msg>

    fun <Msg> getItem(
        key: String,
        toMsg: (String?) -> Msg
    ): Effect<Msg>

    fun <Msg, T> getItemJson(
        key: String,
        decode: Decoder<T>,
        toMsg: (Result<Error, T?>) -> Msg
    ): Effect<Msg>
}

internal class LocalImpl(
    private val localClient: LocalStorage
): Local {
    override fun <Msg> setItem(key: String, value: String, toMsg: () -> Msg): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(id = "LocalStorage.setItem", options = mapOf(key to value)),
            invoke = { dispatch ->
                try {
                    localClient.setItem(key, value)
                    dispatch(toMsg())
                } catch (e: Throwable) {
                    // TODO: add logger
                }
            }
        )
    }

    override fun <Msg> setItemJson(key: String, value: JsonType, toMsg: () -> Msg): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(id = "LocalStorage.setItemJson", options = mapOf(key to value)),
            invoke = { dispatch ->
                try {
                    localClient.setItem(key, value.encode(0))
                    dispatch(toMsg())
                } catch (e: Throwable) {
                    // TODO: add logger
                }
            }
        )
    }

    override fun <Msg> getItem(key: String, toMsg: (String?) -> Msg): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(id = "LocalStorage.getItem", options = mapOf("key" to key)),
            invoke = { dispatch ->
                try {
                    val value = localClient.getItem(key)
                    dispatch(toMsg(value))
                } catch (e: Throwable) {
                    // TODO: add logger
                }
            }
        )
    }



    override fun <Msg, T> getItemJson(
        key: String,
        decoder: Decoder<T>,
        toMsg: (Result<Local.Error, T?>) -> Msg
    ): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(id = "LocalStorage.getItemJson", options = mapOf("key" to key)),
            invoke = {dispatch ->
                try {
                    val value = localClient.getItem(key)
                    val result: Result<Local.Error, T?> = try {
                        val decoded = value?.let { v ->
                            Json.Decode.parse(v, decoder)
                        }
                        when (decoded) {
                            null ->  Result.Success(null)
                            is Result.Success -> Result.Success(decoded.result)
                            is Result.Error -> Result.Error(Local.Error.JsonParseError(Json.Decode.errorToString(decoded.error)))
                        }
                    } catch (e: Throwable) {
                        Result.Error(Local.Error.UnexpectedError(e.message ?: ""))
                    }
                    dispatch(toMsg(result))
                } catch (e: Throwable) {
                    dispatch(toMsg(Result.Error(Local.Error.UnexpectedError(e.message ?: ""))))
                }

            }
        )
    }

}
