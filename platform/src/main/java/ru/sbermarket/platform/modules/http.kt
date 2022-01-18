package ru.sbermarket.platform.modules

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import ru.sbermarket.platform.*
import ru.sbermarket.platform.Meta
import ru.sbermarket.platform.modules.json.Decoder
import ru.sbermarket.platform.modules.json.Json


interface Http {
    fun <Msg> cancel(tracker: String, toMsg: () -> Msg): Effect<Msg>
    fun <Msg> request(
        params: HttpTaskParams<Msg>
    ): Effect<Msg>
    fun <Msg> expectString(toMsg: (Result<Error, String>) -> Msg): Expect<Msg>
    fun <Msg, T> expectJson(
        toMsg: (Result<Error, T>) -> Msg,
        decoder: Decoder<T>
    ): Expect<Msg>

    sealed class Error {
        object Timeout : Error()
        data class BadUrl(val url: String) : Error()
        object NetworkError : Error()
        data class BadStatus(val status: Int): Error()
        data class BadBody(val body: String) : Error()
    }

    data class HttpTaskParams<Msg>(
        val method: String,
        val headers: List<Header> = listOf(),
        val url: String,
        val expect: Expect<Msg>,
        val timeout: Float? = null,
        val tracker: String? = null,
        val body: String? = null
    )
}

internal class HttpImpl(
    private val httpClient: HttpClient
): Http {

    private val jobByTracker = mutableMapOf<String, Job>()

    override fun <Msg> cancel(tracker: String, toMsg: () -> Msg): Effect<Msg> {
        return Effect.ManagedEffect(
            meta = Meta(id = "Http.cancel", options = mapOf("tracker" to tracker)),
            invoke = { dispatch ->
                jobByTracker[tracker]?.cancel()
                jobByTracker.remove(tracker)
                dispatch(toMsg())
            }
        )
    }

    override fun <Msg> request(params: Http.HttpTaskParams<Msg>): Effect<Msg> {
        val call: InvokeEffect<Msg> = { dispatch ->
            val expect = params.expect
            val deferred = async {
                val result = httpClient.request(RequestParams(
                    method = params.method,
                    url = params.url,
                    body = params.body
                ))

                val stringResult: Result<Http.Error, String> =  when (result) {
                    is Result.Error -> Result.Error(result.error)
                    is Result.Success -> {
                        try {
                            val str = result.result.stringBody()
                            Result.Success(str)
                        } catch (e: Throwable) {
                            Result.Error(Http.Error.BadBody(""))
                        }
                    }
                }
                val msg = expect.mapResult(stringResult)
                dispatch(msg)
            }

            params.tracker?.let {
                jobByTracker[it] = deferred
                deferred.await()
                jobByTracker.remove(it)
            }
        }
        return Effect.ManagedEffect(
            invoke = call,
            meta = Meta(
                id = "HTTP_REQUEST_${params.method}",
                options = params.toMap()
            )
        )
    }

    override fun <Msg> expectString(toMsg: (Result<Http.Error, String>) -> Msg): Expect<Msg> {
        return StringExpect(toMsg)
    }

    override fun <Msg, T> expectJson(
        toMsg: (Result<Http.Error, T>) -> Msg,
        decoder: Decoder<T>
    ): Expect<Msg> {
        return JsonExpect(
            toMsg,
            decoder
        )
    }

}

fun Http.HttpTaskParams<*>.toMap(): Map<String, Any?> {
    return mapOf(
        "method" to method,
        "headers" to headers,
        "url" to url,
        "timeout" to timeout,
        "tracker" to tracker,
        "body" to body
    )
}


sealed class Expect<Msg>(
    internal val mapResult: (Result<Http.Error, String>) -> Msg
)
private data class StringExpect<Msg>(
    val toMsg: (Result<Http.Error, String>) -> Msg
): Expect<Msg>(toMsg)
private data class JsonExpect<Msg, T>(
    val toMsg: (Result<Http.Error, T>) -> Msg,
    val decoder: Decoder<T>
): Expect<Msg>(
    mapResult = { result ->
        val next: Result<Http.Error, T> = when (result) {
            is Result.Error -> Result.Error(result.error)
            is Result.Success -> {
                val decoded = Json.Decode.parse(result.result, decoder)
                decoded.mapError {
                    Http.Error.BadBody(Json.Decode.errorToString(it))
                }
            }
        }
        toMsg(next)
    }
)
