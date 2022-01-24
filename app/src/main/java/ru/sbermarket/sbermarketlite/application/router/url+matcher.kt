package ru.sbermarket.sbermarketlite.application.router

import io.ktor.http.*
import io.ktor.util.*
import ru.sbermarket.platform.modules.*


fun Matcher.match(url: Url): MatcherResult {
    return this.match(url.prepare())
}

fun <T> OneOfMatcher<T>.match(url: Url, defaultTo: () -> T): T {
    return match(url.prepare(), defaultTo)
}

fun Url.prepare(): State {
    val paramsMap = parameters.toMap()

    return State(
        visited = this.encodedPath.preparePath(),
        unvisited = listOf(),
        params = Params(
            queryParams = QueryParams(paramsMap)
        )
    )
}
