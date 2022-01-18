package ru.sbermarket.platform



import kotlinx.coroutines.launch


/**
 * Dispatch
 */

/**
 * map dispatch of msg type [A] to dispatch msg type [B]
 */
fun <A, B> contramap(dispatch: Dispatch<A>, f: (B) -> A): Dispatch<B> = { b ->
    dispatch(f(b))
}
/**
 * map dispatch of msg type [A] to dispatch msg type [B]
 */
fun <A, B> Dispatch<A>.mapTo(f : (B) -> A): Dispatch<B> {
    return contramap(this, f)
}

/**
 * Effects
 */

/**
 * Create a empty [Effect]
 */
fun <Msg> none(): Effect<Msg> = Effect.ManagedEffect(
    invoke = {},
    meta = Meta(id = "NONE")
)

/**
 * Compose [effects] into single [Effect]
 */
fun <Msg> batch(vararg effects: Effect<Msg>): Effect<Msg> =
    batch(effects.asIterable())

/**
 * Compose [effects] into a single [Effect].
 */
fun <Msg> batch(effects: Iterable<Effect<Msg>>): Effect<Msg> = Effect.ManagedEffect(
    meta = Meta(id = "BATCH", options = mapOf("children" to effects.map {
        it.meta()
    })),
    invoke = { dispatch ->
        for (effect in effects) {
            launch {
                effect.invoke(this, dispatch)
            }
        }
    }
)

/**
 * Map [effect] of type [A] to [Effect] of [B] using [f].
 */
fun <A, B> map(effect: Effect<A>, f: (A) -> B): Effect<B> = Effect.ManagedEffect(
    meta = effect.meta(),
    invoke = { dispatch ->
        effect.invoke(this) { a -> dispatch(f(a)) }
    }
)
/**
 * Map [effect] of type [A] to [Effect] of [B] using [f].
 */
fun <A, B> Effect<A>.mapTo(f: (A) -> B): Effect<B> {
    return map(this, f)
}


/**
 * Helpers
 */

/**
 * Create a pair of [model] to [effect] where [effect] defaults to [none].
 */
fun <Model, Msg> next(model: Model, effect: Effect<Msg> = none()): Pair<Model, Effect<Msg>> = model to effect

fun <Feature : Any> featureHolder(
    init: (platform: Platform) -> Feature
): (Platform) -> Feature {
    var feature : Feature? = null
    return { platform ->
        feature ?: run {
            val next = init(platform)
            next.also { feat ->
                feature = feat
            }
        }
    }
}
