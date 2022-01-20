package ru.sbermarket.platform

import ru.sbermarket.platform.modules.Http

/**
 * Interface for local key value storage, need to pass your implementation
 * to start work with [Platform]
 */

interface LocalStorage {
    suspend fun setItem(key: String, value: String)
    suspend fun getItem(key: String): String?
}


/**
 * Interface for Http client, need to pass your implementation
 * to start work with [Platform]
 */
interface HttpClient {
    suspend fun request(params: RequestParams): Result<Http.Error, Response>
}
data class Header(
    val name: String,
    val value: String
)

data class RequestParams(
    val method: Http.Method,
    val headers: List<Header> = listOf(),
    val url: String,
    val body: String? = null,
    val timeout: Float? = null
)

interface Response {
    val statusCode: Int
    val stringBody: suspend () -> String
    val headers: List<Header>
}


/**
 * Interface for all dependencies need to pass implementataion
 * to start work with [Platform]
 */
interface PlatformDependencies {
    val http: HttpClient
    val local: LocalStorage
}