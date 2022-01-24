package ru.sbermarket.platform.modules

import ru.sbermarket.platform.Result

/**
 * Routes matcher mechanism:
 * TODO: May be we should add some KSP annotation for match link to Object
 * like Ktor Location [https://ktor.io/docs/locations.html#route-classes]
 *
 *
    sealed class Dest {
        object NotFound : Dest()
        data class Settings(val id: Int, val name: String) : Dest()
        object Messages : Dest()
        object Orders : Dest()
    }


    val matcher = oneOf(
        "user".const() / "id".int() / "settings".const() / "name".string() to {
            val id = it.getInt("id")
            val name = it.getString("name")
            val other = it.queryParams()
            Dest.Settings(id, name)
        },
        "orders".const() to {
            Dest.Orders
        },
        "private".const() / "messages".const() / "as".string() to {
            Dest.Messages
        }
    ).match(state) { Dest.NotFound }


    val state = State(
        unvisited = "user/10/settings/petr".preparePath(),
        visited = listOf(),
        params = Params()
    )

    matcher.match(state) // -> Dest.Settings(id = 10, name = "petr")

    val notFoundState = State(
        unvisited = "not-found".preparePath(),
        visited = listOf(),
        params = Params()
    )
    matcher.match(notFoundState) // -> Dest.None

 */

typealias MatcherResult = Result<Nothing?, State>
interface Matcher {
    fun match(state: State): MatcherResult
}

fun String.preparePath(): List<String> {
    return split("/")
}

data class QueryParams(
    private val map: Map<String, List<String>> = mapOf()
) {
    operator fun get(key: String): String? {
        return map[key]?.firstOrNull()
    }

    fun getAll(key: String): List<String> {
        return map[key] ?: listOf()
    }
}


class Params(
    private val map: Map<String, Any> = mapOf(),
    private val queryParams: QueryParams = QueryParams()
) {
    fun queryParams(): QueryParams {
        return queryParams
    }

    fun getInt(key: String): Int {
        return map[key] as Int
    }
    fun getString(key: String): String {
        return map[key] as String
    }

    fun merge(params: Params): Params {
        return Params(map.plus(params.map))
    }

    override fun toString(): String {
        return map.toString()
    }
}

data class State(
    val visited: List<String>,
    val unvisited: List<String>,
    val params: Params,
    val queryParams: Map<String, String> = mapOf()
)


fun <T : Any> oneOf(vararg matchers: Pair<Matcher, (Params) -> T>): OneOfMatcher<T> {
    return OneOfMatcher(matchers.asList())
}




class OneOfMatcher<T>(
    private val matchers: List<Pair<Matcher, (Params) -> T>>
) {
    fun match(state: State, default: () -> T): T {
        for (pairs in matchers) {
            val result = pairs.first.match(state)
            if (result is Result.Success) {
                return pairs.second(result.result.params)
            }
        }
        return default()
    }
}

fun String.const(): Matcher = StaticMatcher(this)
fun String.int(): Matcher = IntMatcher(this)
fun String.string(): Matcher = StringMatcher(this)

operator fun Matcher.div(
    nextMatcher: Matcher
): Matcher {
    return CompositeMatcher(listOf(this, nextMatcher))
}

internal class CompositeMatcher(
    private val matchers: List<Matcher>
): Matcher {
    override fun match(state: State): MatcherResult {
        var mutableState: State = state
        println(matchers)
        for (matcher in matchers) {
            when (val result = matcher.match(mutableState)) {
                is Result.Error -> {
                    return Result.Error(null)
                }
                is Result.Success -> {
                    mutableState = result.result
                }
            }
        }
        return Result.Success(mutableState)
    }
}

internal class StringMatcher(val name: String) : Matcher {
    override fun match(state: State): MatcherResult {
        return if (state.unvisited.isEmpty()) {
            Result.Error(null)
        } else {
            val head = state.unvisited.first()
            val tail = state.unvisited.drop(1)
            val params = Params(mapOf(name to head))
            val next =  state.copy(
                visited = state.visited.plus(head),
                unvisited = tail,
                params = state.params.merge(params)
            )
            Result.Success(next)
        }

    }
}

internal class IntMatcher(val name: String) : Matcher {
    override fun match(state: State): MatcherResult {
        println("INT_MATCHER")
        println(state)
        return if (state.unvisited.isEmpty()) {
            Result.Error(null)
        } else {
            val head = state.unvisited.first()

            val tail = state.unvisited.drop(1)


            head.toIntOrNull()?.let {
                val params = Params(mapOf(name to it))
                val next = state.copy(
                    visited = state.visited.plus(head),
                    unvisited = tail,
                    params = state.params.merge(params)
                )
                Result.Success(next)
            } ?: Result.Error(null)
        }
    }
}

internal class StaticMatcher(private val pathPart: String): Matcher {
    override fun match(state: State): MatcherResult {
        return if (state.unvisited.isEmpty()) {
            Result.Error(null)
        } else {
            val head = state.unvisited.first()

            val tail = state.unvisited.drop(1)
            val next = state.copy(
                visited = state.visited.plus(head),
                unvisited = tail
            )

            if (head == pathPart) Result.Success(next) else Result.Error(null)
        }
    }
}


