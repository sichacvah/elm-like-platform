package ru.sbermarket.sbermarketlite

interface Lens<A, B>{
    fun get(obj: A): B
    fun set(obj: A, prop: B): A
}

fun <A, B> lens(
    get: (A) -> B,
    set: (A, B) -> A
): Lens<A, B> {
    return object : Lens<A, B> {
        override fun get(obj: A): B = get(obj)
        override fun set(obj: A, prop: B): A = set(obj, prop)
    }
}

fun <A, B, C>Lens<A, B>.compose(bc: Lens<B, C>): Lens<A, C> {
    return lens(
        get = { a -> bc.get(this.get(a)) },
        set = { a, b -> set(a, bc.set(get(a), b)) }
    )
}
