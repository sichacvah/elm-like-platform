package ru.sbermarket.sbermarketlite.application.core

import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import ru.sbermarket.platform.*
import ru.sbermarket.platform.modules.Http
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun HttpResponse.toResponse() = object : Response {
    override val statusCode: Int
        get() = status.value
    override val stringBody: suspend () -> String = {
        receive()
    }
    override val headers: List<Header>
        get() = this@toResponse.headers.toMap().map {
            Header(it.key, it.value.joinToString { "" })
        }

}

class HttpKtorClient: HttpClient {
    private val ktorClient =  io.ktor.client.HttpClient(Android)

    override suspend fun request(params: RequestParams): Result<Http.Error, Response> {
        return try {
            val response: HttpResponse =  ktorClient.request {
                method = HttpMethod.parse(params.method.uppercase())
                headers {
                    params.headers.forEach {
                        append(it.name, it.value)
                    }
                }
                url(params.url)
                params.body?.let { body ->
                    this.body = body
                }
                params.timeout?.let { timeout ->
                    this.timeout {
                        requestTimeoutMillis = timeout.toLong()
                    }
                }
            }
            Result.Success(response.toResponse())
        } catch (e: SocketTimeoutException) {
            Result.Error(Http.Error.Timeout)
        } catch (e: UnknownHostException) {
            Result.Error(Http.Error.BadUrl(params.url))
        } catch (e: ResponseException) {
            Result.Error(Http.Error.BadStatus(e.response.status.value))
        } catch (e: Throwable) {
            Result.Error(Http.Error.NetworkError)
        }
    }

}