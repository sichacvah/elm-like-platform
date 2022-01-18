package ru.sbermarket.platform.modules.json

import ru.sbermarket.platform.Result


object Json {

    internal fun expecting(type: String, value: JsonType): Error.Failure {
        return Error.Failure("Expecting $type", value = value)
    }

    sealed class Error {
        data class Field(
            val key: String,
            val nested: Error
        ) : Error()

        data class Index(
            val index: Int,
            val nested: Error
        ) : Error()

        data class OneOf(
            val errors: List<Error>
        ): Error()

        data class Failure(
            val message: String,
            val value: JsonType
        ): Error()
    }

    object Decode {
        fun <V1, V2, V3, V4, V5, V6, V7, V8, T> convert(
            decoder1: Decoder<V1>,
            decoder2: Decoder<V2>,
            decoder3: Decoder<V3>,
            decoder4: Decoder<V4>,
            decoder5: Decoder<V5>,
            decoder6: Decoder<V6>,
            decoder7: Decoder<V7>,
            decoder8: Decoder<V8>,
            convert: (V1, V2, V3, V4, V5, V6, V7, V8) -> T
        ): Decoder<T> {
            return Map8Decoder(decoder1, decoder2, decoder3, decoder4, decoder5, decoder6, decoder7, decoder8, convert)
        }

        fun <V1, V2, V3, V4, V5, V6, V7, T> convert(
            decoder1: Decoder<V1>,
            decoder2: Decoder<V2>,
            decoder3: Decoder<V3>,
            decoder4: Decoder<V4>,
            decoder5: Decoder<V5>,
            decoder6: Decoder<V6>,
            decoder7: Decoder<V7>,
            convert: (V1, V2, V3, V4, V5, V6, V7) -> T
        ): Decoder<T> {
            return Map7Decoder(decoder1, decoder2, decoder3, decoder4, decoder5, decoder6, decoder7, convert)
        }

        fun <V1, V2, V3, V4, V5, V6, T> convert(
            decoder1: Decoder<V1>,
            decoder2: Decoder<V2>,
            decoder3: Decoder<V3>,
            decoder4: Decoder<V4>,
            decoder5: Decoder<V5>,
            decoder6: Decoder<V6>,
            convert: (V1, V2, V3, V4, V5, V6) -> T
        ): Decoder<T> {
            return Map6Decoder(decoder1, decoder2, decoder3, decoder4, decoder5, decoder6, convert)
        }

        fun <V1, V2, V3, V4, V5, T> convert(
            decoder1: Decoder<V1>,
            decoder2: Decoder<V2>,
            decoder3: Decoder<V3>,
            decoder4: Decoder<V4>,
            decoder5: Decoder<V5>,
            convert: (V1, V2, V3, V4, V5) -> T
        ): Decoder<T> {
            return Map5Decoder(decoder1, decoder2, decoder3, decoder4, decoder5, convert)
        }

        fun <V1, V2, V3, V4, T> convert(
            decoder1: Decoder<V1>,
            decoder2: Decoder<V2>,
            decoder3: Decoder<V3>,
            decoder4: Decoder<V4>,
            convert: (V1, V2, V3, V4) -> T
        ): Decoder<T> {
            return Map4Decoder(decoder1, decoder2, decoder3, decoder4, convert)
        }

        fun <V1, V2, V3, T> convert(
            decoder1: Decoder<V1>,
            decoder2: Decoder<V2>,
            decoder3: Decoder<V3>,
            convert: (V1, V2, V3) -> T
        ): Decoder<T> {
            return Map3Decoder(decoder1, decoder2, decoder3, convert)
        }

        fun <V1, V2, T> convert(
            decoder1: Decoder<V1>,
            decoder2: Decoder<V2>,
            convert: (V1, V2) -> T
        ): Decoder<T> {
            return Map2Decoder(decoder1, decoder2, convert)
        }

        fun <V1, T> convert(
            decoder1: Decoder<V1>,
            convert: (V1) -> T
        ): Decoder<T> {
            return Map1Decoder(decoder1, convert)
        }

        fun <T> success(value: T): Decoder<T> {
            return SucceedDecoder(value)
        }

        fun <T> fail(msg: String): Decoder<T> {
            return FailDecoder(msg)
        }

        fun <A, B> andThen(decoder: Decoder<A>, converter: (A) -> Decoder<B>): Decoder<B> {
            return AndThenDecoder(converter, decoder)
        }

        fun value(): Decoder<JsonType> {
            return ValueDecoder
        }

        fun <T> decodeNull(value: T): Decoder<T> {
            return NullDecoder(value)
        }

        private fun indent(str: String): String = str.split("\n").joinToString("\n    ")
        private fun errorOneOf(index: Int, error: Error) = "\n\n(${index + 1})${indent(errorToString(error))}"

        private tailrec fun errorToStringHelp(error: Error, context: List<String>): String {
            return when (error) {
                is Error.Field -> {
                    val isSimple = error.key.head?.let {
                        it.isUpperCase() && error.key.tail.all { char -> char.isUpperCase() || char.isDigit() }
                    } ?: false
                    val fieldName = if (isSimple) "." + error.key else "['${error.key}']"
                    errorToStringHelp(error.nested, context.plus(fieldName))
                }
                is Error.Index -> {
                    val indexName = "[${error.index}]"
                    errorToStringHelp(error.nested, context.plus(indexName))
                }
                is Error.OneOf -> {
                    val errors = error.errors
                    when (errors.size) {
                        0 -> "Ran into a Json.Decode.oneOf with no possibilities" + if (context.isEmpty()) "!" else " at json" + context.reversed().joinToString()
                        1 -> errorToStringHelp(error.errors.first(), context)
                        else -> {
                            val starter = if (context.isEmpty()) "Json.Decode.oneOf" else "The Json.Decode.oneOf at json" + context.reversed().joinToString()
                            val introduction = "$starter failed in the following ${error.errors.size} ways:"
                            listOf(introduction).plus(error.errors.mapIndexed { index, error -> errorOneOf(index, error) }).joinToString("\n\n")
                        }
                    }
                }
                is Error.Failure -> {
                    val introduction = if (context.isNotEmpty()) {
                        "Problem with the given value:\\n\\n"
                    } else {
                        "Problem with the value at json ${context.reversed().joinToString()} :\n\n"
                    }
                    "$introduction ${indent(Encode.encode(4, error.value))} \n\n ${error.message}"
                }
            }
        }

        fun errorToString(error: Error): String {
            return errorToStringHelp(error, listOf())
        }

        fun <T> parse(string: String, decoder: Decoder<T>): Result<Error, T> {
            return when (val parsed = JsonParse.parse(string)) {
                is Result.Success -> decoder.decode(parsed.result)
                is Result.Error -> {
                    Result.Error(Error.Failure("This is not valid JSON! ${parsed.error}", JsonString(string)))
                }
            }
        }

        fun parse(string: String): Result<JsonParse.JsonParseError, JsonType> {
            return JsonParse.parse(string)
        }

        fun string(): Decoder<String> = StringDecoder

        fun float(): Decoder<Float> = FloatDecoder

        fun long(): Decoder<Long> = LongDecoder

        fun int(): Decoder<Int> = IntDecoder

        fun boolean(): Decoder<Boolean> = BooleanDecoder

        fun <T> nullable(decoder: Decoder<T>): Decoder<T?> = NullableDecoder(decoder)

        fun <T> list(itemDecoder: Decoder<T>): Decoder<List<T>> = ListDecoder(itemDecoder)

        fun <T> keyValueDecoder(valueDecoder: Decoder<T>): Decoder<List<Pair<String, T>>> = KeyValueDecoder(valueDecoder)

        fun <T> mapDecoder(valueDecoder: Decoder<T>): Decoder<Map<String, T>> {
            val decoder = KeyValueDecoder(valueDecoder)
            return convert(decoder) {
                it.toMap()
            }
        }

        fun <T> field(key: String, decoder: Decoder<T>): Decoder<T> {
            return FieldDecoder(key, decoder)
        }

        fun <T> at(path: List<String>, decoder: Decoder<T>): Decoder<T> {
            return path.foldRight(decoder) { key, acc ->
                field(key, acc)
            }
        }

        fun <T> index(index: Int, decoder: Decoder<T>): Decoder<T> {
            return IndexDecoder(index, decoder)
        }
    }

    object Encode {
        fun encode(indent: Int, value: JsonType): String {
            return value.encode(indent)
        }

        fun string(string: String): JsonType {
            return JsonString(string)
        }

        fun int(int: Int): JsonType {
            return JsonInt(int)
        }

        fun long(long: Long): JsonType {
            return JsonLong(long)
        }

        fun boolean(boolean: Boolean): JsonType {
            return JsonBoolean(boolean)
        }

        fun makeNull(): JsonType {
            return JsonNull
        }

        fun float(float: Float): JsonType = JsonFloat(float)

        fun <T> list(list: List<T>, block: (T) -> JsonType): JsonType {
            return JsonArray(list.map(block))
        }

        fun <T> obj(pairs: List<Pair<String, JsonType>>): JsonType {
            val map = pairs.fold(mapOf<String, JsonType>()) { acc, pair ->
                acc.plus(pair)
            }
            return JsonObject(map)
        }

        fun <K, V> map(toKey: (K) -> String, toValue: (V) -> JsonType, map: Map<K, V>): JsonType {
            val obj = map.map { entry ->
                toKey(entry.key) to toValue(entry.value)
            }.toMap()

            return JsonObject(obj)
        }
    }

}



