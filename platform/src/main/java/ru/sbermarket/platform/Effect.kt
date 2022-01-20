package ru.sbermarket.platform

import kotlinx.coroutines.CoroutineScope

/**
 * A function to dispatch a Msg to the runtime in the runtime context.
 * Similar to redux dispatch
 */
typealias Dispatch<Msg> = (Msg) -> Unit


/**
 *  A function to run a side-effect in the effect context. Provides a Dispatch function to dispatch a Msg to the
 *  runtime in the runtime context.
 *  Similar to redux-thunk
 */
internal typealias InvokeEffect<Msg> = suspend CoroutineScope.(Dispatch<Msg>) -> Any?

/**
 * Metadata for effect comparing
 */
internal data class Meta(
    val id: String,
    val options: Map<String, Any?> = mapOf()
)

/**
 * getter for [Meta]
 */
internal fun <Msg> Effect<Msg>.meta(): Meta {
    return when (this) {
        is Effect.ManagedEffect -> meta
    }
}

/**
 * getter for [InvokeEffect]
 */
internal val <Msg> Effect<Msg>.invoke: InvokeEffect<Msg>
    get() = when (this) {
        is Effect.ManagedEffect -> invoke
    }


sealed class Effect<Msg> {
    internal class ManagedEffect<Msg>(
        internal val invoke : InvokeEffect<Msg>,
        internal val meta : Meta
    ) : Effect<Msg>() {
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is ManagedEffect<*>) return false
            return this.meta == other.meta
        }

        override fun hashCode(): Int {
            return meta.hashCode()
        }
    }
}