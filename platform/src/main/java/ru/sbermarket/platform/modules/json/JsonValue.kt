package ru.sbermarket.platform.modules.json

import ru.sbermarket.platform.Result


sealed class JsonValue<T> {
    abstract val value : T
    open fun makeString(indent: Int): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) || value == other ||
                other is JsonValue<*> && value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    fun asFloat(): Result<Json.Error, Float> {
        return when (this) {
            is JsonFloat -> Result.Success(value)
            else -> Result.Error(Json.expecting("a FLOAT", this))
        }
    }

    fun asInt(): Result<Json.Error, Int> {
        return when (this) {
            is JsonInt -> Result.Success(value)
            else -> Result.Error(Json.expecting("an INT", this))
        }
    }

    fun asLong(): Result<Json.Error, Long> {
        return when (this) {
            is JsonLong -> Result.Success(value)
            else -> Result.Error(Json.expecting("a LONG", this))
        }
    }

    fun asBoolean(): Result<Json.Error, Boolean> {
        return when (this) {
            is JsonBoolean -> Result.Success(value)
            else -> Result.Error(Json.expecting("a BOOL", this))
        }
    }

    fun asString(): Result<Json.Error, String> {
        return when (this) {
            is JsonString -> Result.Success(value)
            else -> Result.Error(Json.expecting("a STRING", this))
        }
    }

    fun asMap(): Result<Json.Error, Map<String, JsonType>> {
        return when (this) {
            is JsonObject -> Result.Success(value)
            else -> Result.Error(Json.expecting("a MAP", this))
        }
    }

    fun asList(): Result<Json.Error, List<JsonType>> {
        return when (this) {
            is JsonArray -> Result.Success(value)
            else -> Result.Error(Json.expecting("a LIST", this))
        }
    }

    fun asNull(): Result<Json.Error, Nothing?> {
        return when (this) {
            is JsonNull -> Result.Success(null)
            else -> Result.Error(Json.expecting("a NULL", this))
        }
    }

    fun encode(indent: Int = 0): String {
        return makeString(indent)
    }

}


object JsonNull : JsonValue<Nothing?>() {
    override val value = null

}

data class JsonInt(override val value : Int) : JsonValue<Int>()
data class JsonLong(override val value : Long) : JsonValue<Long>()
data class JsonFloat(override val value : Float) : JsonValue<Float>()
data class JsonBoolean(override val value : Boolean) : JsonValue<Boolean>()
data class JsonString(override val value : String) : JsonValue<String>() {
    override fun makeString(indent: Int) = "\"${value.jsonUnescape()}\""
}

typealias JsonType = JsonValue<*>

data class JsonArray(override val value: List<JsonType>): JsonValue<List<JsonType>>() {
    operator fun get(index: Int) = value.getOrNull(index) ?: JsonNull
    override fun makeString(indent: Int): String {
        val n = if(indent > 0) "\n" else ""
        return value.joinToString(",$n", "[$n", "$n]") { it.makeString(indent).indent(indent) }
    }

    override fun equals(other: Any?): Boolean {
        if(super.equals(other)) return true
        if(other !is JsonArray) return false
        return value.zip(other.value).all { it.first == it.second }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
data class JsonObject(override val value: Map<String, JsonType>): JsonValue<Map<String, JsonType>>() {
    operator fun get(key: String) = value.getOrDefault(key){JsonNull}

    override fun makeString(indent: Int): String {
        val n = if (indent > 0) "\n" else ""
        val s = if (indent > 0) " " else ""
        return value
            .map { (k, v) -> "\"$k\":$s${v.makeString(indent)}".indent(indent) }
            .joinToString(",$n", "{$n", "$n}")
    }


    override fun equals(other: Any?): Boolean {
        if(super.equals(other)) return true
        if(other !is JsonObject) return false
        return value.entries.zip(other.value.entries).all {
            it.first.key == it.second.key && it.first == it.second
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}









