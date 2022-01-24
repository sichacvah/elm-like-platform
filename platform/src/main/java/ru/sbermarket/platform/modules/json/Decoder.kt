package ru.sbermarket.platform.modules.json

import ru.sbermarket.platform.Result


sealed class Decoder<T> {
    abstract fun decode(value: JsonType): Result<Json.Error, T>
}

internal object ValueDecoder : Decoder<JsonType>() {
    override fun decode(value: JsonType): Result<Json.Error, JsonType> {
        return Result.Success(value)
    }
}

internal object BooleanDecoder : Decoder<Boolean>() {
    override fun decode(value: JsonType): Result<Json.Error, Boolean> {
        return value.asBoolean()
    }
}

internal object FloatDecoder : Decoder<Float>() {
    override fun decode(value: JsonType): Result<Json.Error, Float> {
        return value.asFloat()
    }
}

internal object IntDecoder : Decoder<Int>() {
    override fun decode(value: JsonType): Result<Json.Error, Int> {
        return value.asInt()
    }
}

internal object LongDecoder : Decoder<Long>() {
    override fun decode(value: JsonType): Result<Json.Error, Long> {
        return value.asLong()
    }
}

internal object StringDecoder : Decoder<String>() {
    override fun decode(value: JsonType): Result<Json.Error, String> {
        return value.asString()
    }
}

internal class NullDecoder<T>(private val fallback : T): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        return when (val result = value.asNull()) {
            is Result.Success -> Result.Success(fallback)
            is Result.Error -> Result.Error(result.error)
        }
    }
}

internal class NullableDecoder<T>(
    private val decoder: Decoder<T>
) : Decoder<T?>() {
    override fun decode(value: JsonType): Result<Json.Error, T?> {
        return when (value.asNull()) {
            is Result.Success -> Result.Success(null)
            is Result.Error -> {
                when (val decoded = decoder.decode(value)) {
                    is Result.Error -> Result.Error(decoded.error)
                    is Result.Success -> Result.Success(decoded.result)
                }
            }
        }
    }
}

internal class KeyValueDecoder<T>(
    private val elementDecoder: Decoder<T>
): Decoder<List<Pair<String, T>>>() {
    override fun decode(value: JsonType): Result<Json.Error, List<Pair<String, T>>> {

        when (val mapResult = value.asMap()) {
            is Result.Error -> return Result.Error(mapResult.error)
            is Result.Success -> {
                val elements = mutableListOf<Pair<String, T>>()
                val entries = mapResult.result.entries
                for (entry in entries) {
                    when (val result = elementDecoder.decode(entry.value)) {
                        is Result.Success -> {
                            elements.add(entry.key to result.result)
                        }
                        is Result.Error -> {
                            return Result.Error(Json.Error.Field(entry.key, result.error))
                        }
                    }
                }
                return Result.Success(elements)
            }
        }
    }
}

internal class ListDecoder<T>(
    private val elementDecoder: Decoder<T>
): Decoder<List<T>>() {
    override fun decode(value: JsonType): Result<Json.Error, List<T>> {
        when (val listResult = value.asList()) {
            is Result.Error -> return Result.Error(listResult.error)
            is Result.Success -> {
                val list = listResult.result
                val elements = mutableListOf<T>()
                for (i in list.indices) {
                    val item = list[i]
                    when (val result = elementDecoder.decode(item)) {
                        is Result.Error -> return Result.Error(Json.Error.Index(i, result.error))
                        is Result.Success -> {
                            elements.add(result.result)
                        }
                    }

                }
                return Result.Success(elements)
            }
        }
    }
}

internal class OneOfDecoder<T>(
    private val decoders: List<Decoder<T>>
): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val errors = mutableListOf<Json.Error>()
        for (decoder in decoders) {
            when (val result = decoder.decode(value)) {
                is Result.Error -> errors.add(result.error)
                is Result.Success -> {
                    return Result.Success(result.result)
                }
            }
        }
        return Result.Error(Json.Error.OneOf(errors))
    }

}

internal class Map1Decoder<V1, T>(
    private val decoder: Decoder<V1>,
    val map: (v1 : V1) -> T
): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        return when (val value1Result = decoder.decode(value)) {
            is Result.Error -> Result.Error(value1Result.error)
            is Result.Success -> Result.Success(map(value1Result.result))
        }
    }
}

internal class Map2Decoder<V1, V2, T>(
    private val decoder1: Decoder<V1>,
    private val decoder2: Decoder<V2>,
    val map: (V1, V2) -> T
    ): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        return when (val value1Result = decoder1.decode(value)) {
            is Result.Error -> {
                Result.Error(value1Result.error)
            }
            is Result.Success -> {
                when (val value2Result = decoder2.decode(value)) {
                    is Result.Error -> {
                        Result.Error(value2Result.error)
                    }
                    is Result.Success -> {
                        Result.Success(map.invoke(value1Result.result, value2Result.result))
                    }
                }
            }
        }
    }
}

internal class Map3Decoder<V1, V2, V3, T>(
    private val decoder1: Decoder<V1>,
    private val decoder2: Decoder<V2>,
    private val decoder3: Decoder<V3>,
    val map: (V1, V2, V3) -> T
    ): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val partialMap: (V1, V2) -> (V3) -> T = { v1, v2 ->
            { v3 ->
                map(v1, v2, v3)
            }
        }

        val map2Decoder = Map2Decoder(decoder1, decoder2, partialMap)

        return when (val map2Result = map2Decoder.decode(value)) {
            is Result.Error -> {
                Result.Error(map2Result.error)
            }
            is Result.Success -> {
                val map1Decoder = Map1Decoder(decoder3, map2Result.result)
                when (val result = map1Decoder.decode(value)) {
                    is Result.Error -> {
                        Result.Error(result.error)
                    }
                    is Result.Success -> {
                        Result.Success(result.result)
                    }
                }
            }
        }
    }
}


internal class Map4Decoder<V1, V2, V3, V4, T>(
    private val decoder1: Decoder<V1>,
    private val decoder2: Decoder<V2>,
    private val decoder3: Decoder<V3>,
    private val decoder4: Decoder<V4>,
    val map: (V1, V2, V3, V4) -> T
): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val partialMap: (V1, V2) -> (V3, V4) -> T = { v1, v2 ->
            { v3, v4 ->
                map(v1, v2, v3, v4)
            }
        }

        val map2Decoder = Map2Decoder(decoder1, decoder2, partialMap)

        return when (val map2Result = map2Decoder.decode(value)) {
            is Result.Error -> {
                Result.Error(map2Result.error)
            }
            is Result.Success -> {
                val finalDecoder = Map2Decoder(decoder3, decoder4, map2Result.result)
                when (val result = finalDecoder.decode(value)) {
                    is Result.Error -> {
                        Result.Error(result.error)
                    }
                    is Result.Success -> {
                        Result.Success(result.result)
                    }
                }
            }
        }
    }
}

internal class Map5Decoder<V1, V2, V3, V4, V5, T>(
    private val decoder1: Decoder<V1>,
    private val decoder2: Decoder<V2>,
    private val decoder3: Decoder<V3>,
    private val decoder4: Decoder<V4>,
    private val decoder5: Decoder<V5>,
    val map: (V1, V2, V3, V4, V5) -> T
): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val partialMap: (V1, V2) -> (V3, V4, V5) -> T = { v1, v2 ->
            { v3, v4, v5 ->
                map(v1, v2, v3, v4, v5)
            }
        }

        val map2Decoder = Map2Decoder(decoder1, decoder2, partialMap)

        return when (val map2Result = map2Decoder.decode(value)) {
            is Result.Error -> {
                Result.Error(map2Result.error)
            }
            is Result.Success -> {
                val finalDecoder = Map3Decoder(decoder3, decoder4, decoder5, map2Result.result)
                when (val result = finalDecoder.decode(value)) {
                    is Result.Error -> {
                        Result.Error(result.error)
                    }
                    is Result.Success -> {
                        Result.Success(result.result)
                    }
                }
            }
        }
    }
}

internal class Map6Decoder<V1, V2, V3, V4, V5, V6, T>(
    private val decoder1: Decoder<V1>,
    private val decoder2: Decoder<V2>,
    private val decoder3: Decoder<V3>,
    private val decoder4: Decoder<V4>,
    private val decoder5: Decoder<V5>,
    private val decoder6: Decoder<V6>,
    val map: (V1, V2, V3, V4, V5, V6) -> T
): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val partialMap: (V1, V2, V3) -> (V4, V5, V6) -> T = { v1, v2, v3 ->
            { v4, v5, v6 ->
                map(v1, v2, v3, v4, v5, v6)
            }
        }

        val firstStepDecoder = Map3Decoder(decoder1, decoder2, decoder3, partialMap)

        return when (val firstResult = firstStepDecoder.decode(value)) {
            is Result.Error -> {
                Result.Error(firstResult.error)
            }
            is Result.Success -> {
                val finalDecoder = Map3Decoder(decoder4, decoder5, decoder6, firstResult.result)
                when (val result = finalDecoder.decode(value)) {
                    is Result.Error -> {
                        Result.Error(result.error)
                    }
                    is Result.Success -> {
                        Result.Success(result.result)
                    }
                }
            }
        }
    }
}

internal class Map7Decoder<V1, V2, V3, V4, V5, V6, V7, T>(
    private val decoder1: Decoder<V1>,
    private val decoder2: Decoder<V2>,
    private val decoder3: Decoder<V3>,
    private val decoder4: Decoder<V4>,
    private val decoder5: Decoder<V5>,
    private val decoder6: Decoder<V6>,
    private val decoder7: Decoder<V7>,
    val map: (V1, V2, V3, V4, V5, V6, V7) -> T
): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val partialMap: (V1, V2, V3) -> (V4, V5, V6, V7) -> T = { v1, v2, v3 ->
            { v4, v5, v6, v7 ->
                map(v1, v2, v3, v4, v5, v6, v7)
            }
        }

        val firstStepDecoder = Map3Decoder(decoder1, decoder2, decoder3, partialMap)

        return when (val firstResult = firstStepDecoder.decode(value)) {
            is Result.Error -> {
                Result.Error(firstResult.error)
            }
            is Result.Success -> {
                val finalDecoder = Map4Decoder(decoder4, decoder5, decoder6, decoder7, firstResult.result)
                when (val result = finalDecoder.decode(value)) {
                    is Result.Error -> {
                        Result.Error(result.error)
                    }
                    is Result.Success -> {
                        Result.Success(result.result)
                    }
                }
            }
        }
    }
}


internal class Map8Decoder<V1, V2, V3, V4, V5, V6, V7, V8, T>(
    private val decoder1: Decoder<V1>,
    private val decoder2: Decoder<V2>,
    private val decoder3: Decoder<V3>,
    private val decoder4: Decoder<V4>,
    private val decoder5: Decoder<V5>,
    private val decoder6: Decoder<V6>,
    private val decoder7: Decoder<V7>,
    private val decoder8: Decoder<V8>,
    val map: (V1, V2, V3, V4, V5, V6, V7, V8) -> T
): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val partialMap: (V1, V2, V3, V4) -> (V5, V6, V7, V8) -> T = { v1, v2, v3, v4 ->
            { v5, v6, v7, v8 ->
                map(v1, v2, v3, v4, v5, v6, v7, v8)
            }
        }

        val firstStepDecoder = Map4Decoder(decoder1, decoder2, decoder3, decoder4, partialMap)

        return when (val firstResult = firstStepDecoder.decode(value)) {
            is Result.Error -> {
                Result.Error(firstResult.error)
            }
            is Result.Success -> {
                val finalDecoder = Map4Decoder(decoder5, decoder6, decoder7, decoder8, firstResult.result)
                when (val result = finalDecoder.decode(value)) {
                    is Result.Error -> {
                        Result.Error(result.error)
                    }
                    is Result.Success -> {
                        Result.Success(result.result)
                    }
                }
            }
        }
    }
}

internal class SucceedDecoder<T>(private val valueByDefault: T): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        return Result.Success(valueByDefault)
    }
}

internal class FailDecoder<T>(private val msg: String): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val error = Json.Error.Failure(msg, value)
        return Result.Error(error)
    }
}


internal class IndexDecoder<T>(private val index: Int, private val decoder: Decoder<T>): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {

        when (val listResult = value.asList()) {
            is Result.Error -> return Result.Error(Json.expecting("an ARRAY", value))
            is Result.Success -> {
                val list = listResult.result
                if (index >= list.size) {
                    return Result.Error(Json.expecting("a LONGER array. Need index $index but only see ${list.size}", value))
                }

                return when (val result = decoder.decode(list[index])) {
                    is Result.Error -> Result.Error(Json.Error.Index(index, result.error))
                    is Result.Success -> Result.Success(result.result)
                }
            }
        }
    }
}

internal class FieldDecoder<T>(private val field: String, private val decoder: Decoder<T>): Decoder<T>() {
    override fun decode(value: JsonType): Result<Json.Error, T> {
        val error: Json.Error.Failure = Json.expecting(
            "an OBJECT with a field named ` $field `",
            value
        )
        when (val mapResult = value.asMap()) {
            is Result.Error -> return Result.Error(error)
            is Result.Success -> {
                val mapValue = mapResult.result[field] ?: return Result.Error(error)

                return when (val result = decoder.decode(mapValue)) {
                    is Result.Error -> Result.Error(Json.Error.Field(field, result.error))
                    is Result.Success -> Result.Success(result.result)
                }
            }
        }
    }
}

internal class AndThenDecoder<A, B>(
    val next : (A) -> Decoder<B>,
    val decoder: Decoder<A>
): Decoder<B>() {
    override fun decode(value: JsonType): Result<Json.Error, B> {
        return when (val result = decoder.decode(value)) {
            is Result.Error -> Result.Error(result.error)
            is Result.Success -> next(result.result).decode(value)
        }
    }
}
