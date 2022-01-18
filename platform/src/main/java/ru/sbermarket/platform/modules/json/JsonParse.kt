package ru.sbermarket.platform.modules.json

import ru.sbermarket.platform.Result


internal object ParseIntError
internal object ParseLongError
internal object ParseFloatError

internal fun parseFloat(str: String): Result<ParseFloatError, Float> {
    return try {
        Result.Success(str.toFloat())
    } catch (e: Throwable) {
        Result.Error(ParseFloatError)
    }
}

internal fun parseInt(str: String): Result<ParseIntError, Int> {
    return try {
        Result.Success(str.toInt())
    } catch (e: Throwable) {
        Result.Error(ParseIntError)
    }
}

internal fun parseLong(str: String): Result<ParseLongError, Long> {
    return try {
        Result.Success(str.toLong())
    } catch (e: Throwable) {
        Result.Error(ParseLongError)
    }
}


object JsonParse {

    private sealed class Token(val text: kotlin.String) {
        object ObjectBegin : Token("{")
        object ObjectEnd : Token("}")
        object ArrayBegin : Token("[")
        object ArrayEnd : Token("]")
        object Comma : Token(",")
        object Colon : Token(":")
        object True : Token("true")
        object False : Token("false")
        object Null : Token("null")

        class Int(text: kotlin.String) : Token(text)
        class Float(text: kotlin.String) : Token(text)
        class String(text: kotlin.String) : Token(text)

        override fun toString() = text
    }

    sealed class JsonParseError : Throwable() {
        object UnexpectedEOF : JsonParseError()
        data class UnexpectedChar(val string : String) : JsonParseError()
        data class WrongObjectStructure(val obj: String) : JsonParseError()
        data class CannotParseNumber(val string : String) : JsonParseError()
        data class UnexpectedCharsBeforeEOF(val string : String) : JsonParseError()
    }

    fun parse(from: String): Result<JsonParseError, JsonType> {
        return when (val lexResult = lex(from)) {
            is Result.Error -> {
                Result.Error(lexResult.error)
            }
            is Result.Success -> {
                when (val result = parse(lexResult.result)) {
                    is Result.Error -> {
                        Result.Error(result.error)
                    }
                    is Result.Success -> {
                        val (json, tail) = result.result
                        if (tail.isNotEmpty()) {
                            Result.Error(JsonParseError.UnexpectedCharsBeforeEOF(tail.joinToString()))
                        } else {
                            Result.Success(json)
                        }
                    }
                }
            }
        }
    }

    private fun parse(tokens: List<Token>): Result<JsonParseError, Pair<JsonType, List<Token>>> {
        when (tokens.head) {
            Token.ObjectBegin -> {
                var tail = tokens.tail
                val pairs: MutableList<Pair<String, JsonType>> = mutableListOf()
                while (tail.head !is Token.ObjectEnd) {
                    if (tail.size < 4) {
                        return Result.Error(JsonParseError.UnexpectedEOF)
                    }
                    val key = tail.head
                    if (key == null || key !is Token.String || tail.drop(1).head !is Token.Colon) {
                        return Result.Error(JsonParseError.WrongObjectStructure(tokens.head.toString()))
                    }
                    when (val next = parse(tail.drop(2))) {
                        is Result.Error -> {
                            return Result.Error(next.error)
                        }
                        is Result.Success -> {
                            val (value, newTail) = next.result
                            pairs += key.text to value
                            tail = if (newTail.head is Token.Comma) newTail.tail else newTail
                        }
                    }
                }
                return Result.Success(JsonObject(pairs.toMap()) to tail.tail)
            }
            Token.ArrayBegin -> {
                var tail = tokens.tail
                var list: List<JsonType> = emptyList()
                while (tail.head !is Token.ArrayEnd) {
                    if (tail.size < 4)
                        return Result.Error(JsonParseError.UnexpectedEOF)


                    when (val next = parse(tail)) {
                        is Result.Error -> return Result.Error(next.error)
                        is Result.Success -> {
                            val (value, newTail) = next.result
                            list = list.plus(value)
                            tail = if (newTail.head is Token.Comma) newTail.tail else newTail
                        }
                    }
                }
                return Result.Success(JsonArray(list) to tail.tail)
            }
            Token.True -> return Result.Success(JsonBoolean(true) to tokens.tail)
            Token.False -> return Result.Success(JsonBoolean(false) to tokens.tail)
            Token.Null -> return Result.Success(JsonNull to tokens.tail)
            is Token.Int -> {
                val maybeHead = tokens.head
                return maybeHead?.let {
                    when (val intResult = parseInt(it.text)) {
                        is Result.Success -> Result.Success(JsonInt(intResult.result) to tokens.tail)
                        is Result.Error -> {
                            when (val longResult = parseLong(it.text)) {
                                is Result.Success -> Result.Success(JsonLong(longResult.result) to tokens.tail)
                                is Result.Error -> Result.Error(JsonParseError.CannotParseNumber(it.text))
                            }
                        }
                    }
                } ?: Result.Error(JsonParseError.UnexpectedEOF)
            }
            is Token.Float -> {
                val maybeHead = tokens.head
                return maybeHead?.let { head ->
                    when (val floatResult = parseFloat(head.text)) {
                        is Result.Error -> Result.Error(JsonParseError.CannotParseNumber(head.text))
                        is Result.Success -> Result.Success(JsonFloat(floatResult.result) to tokens.tail)
                    }
                } ?: Result.Error(JsonParseError.UnexpectedEOF)
            }
            is Token.String -> {
                return tokens.head?.let { head ->
                    Result.Success(JsonString(head.text.jsonUnescape()) to tokens.tail)
                } ?: Result.Error(JsonParseError.UnexpectedEOF)
            }
            else ->
                return Result.Error(JsonParseError.UnexpectedCharsBeforeEOF(tokens.head.toString()))
        }
    }

    private tailrec fun lex(from: String, tokens: List<Token> = emptyList()): Result<JsonParseError, List<Token>> = when (from.head ?: "".head) {
        null -> Result.Success(tokens)
        in Regex("\\s") -> lex(from.trim(), tokens)
        '{' -> lex(from.tail, tokens + Token.ObjectBegin)
        '}' -> lex(from.tail, tokens + Token.ObjectEnd)
        '[' -> lex(from.tail, tokens + Token.ArrayBegin)
        ']' -> lex(from.tail, tokens + Token.ArrayEnd)
        ',' -> lex(from.tail, tokens + Token.Comma)
        ':' -> lex(from.tail, tokens + Token.Colon)
        '"' -> {
            val str = run loop@{ from.tail.fold("") { acc, c ->
                if (c == '"' && !acc.endsWith('\\')) {
                    return@loop acc
                }
                acc + c
            }}
            lex(from.drop(str.length + 2), tokens + Token.String(str))
        }
        in Regex("\\d"), '-' -> {
            val num = run loop@{ from.tail.fold(from.head.toString()) { acc, c ->
                if(c == '-' || c in Regex("[.eE]") && acc.contains(c, true)) {
                    return Result.Error(JsonParseError.UnexpectedChar(c.toString()))
                }
                if(c !in Regex("[.eE\\d]"))
                    return@loop acc
                acc + c
            } }

            if(num.contains(Regex("[.eE]"))) lex(from.drop(num.length), tokens + Token.Float(num))
            else lex(from.drop(num.length), tokens + Token.Int(num))
        }
        else -> when {
            from.startsWith("true") -> lex(from.drop(4), tokens + Token.True)
            from.startsWith("false") -> lex(from.drop(5), tokens + Token.False)
            from.startsWith("null") -> lex(from.drop(4), tokens + Token.Null)
            else -> Result.Error(JsonParseError.UnexpectedChar(from.head.toString()))
        }
    }
}