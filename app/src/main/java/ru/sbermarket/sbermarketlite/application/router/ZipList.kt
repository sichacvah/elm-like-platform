package ru.sbermarket.sbermarketlite.application.router

/**
 * Data structure similar to list but can't be empty,
 * at least one element always in list
 */
sealed interface ZipList<Item>

internal data class ZipListImpl<Item>(
    internal val head: Item,
    internal val tail: List<Item>
): ZipList<Item>

fun <Item> ziplist(
    head: Item,
    tail: List<Item> = listOf()
): ZipList<Item> = ZipListImpl(
    head,
    tail
)

fun <A> ZipList<A>.pop(): Pair<ZipList<A>, A?> {
    val head = head()
    val tail = tail()
    return if (tail.isEmpty()) {
        this to null
    } else {
        ziplist(head = tail.first(), tail = tail.drop(1)) to head
    }
}

fun <A> ZipList<A>.push(a: A): ZipList<A> {
    return when (this) {
        is ZipListImpl -> ziplist(
            head = a,
            tail = tail.plus(head)
        )
    }
}

fun <A, B> ZipList<A>.map(t: (A) -> B): ZipList<B> {
    return when (this) {
        is ZipListImpl -> ziplist(
            head = t(head),
            tail = tail.map(t)
        )
    }
}

fun <A> ZipList<A>.asList(): List<A> {
    return when (this) {
        is ZipListImpl -> listOf(this.head) + this.tail
    }
}

fun <A> ZipList<A>.mapTail(t: (A) -> A): ZipList<A> {
    return when (this) {
        is ZipListImpl -> ziplist(
            head = head,
            tail = tail.map(t)
        )
    }
}

fun <A> ZipList<A>.mapHead(t: (A) -> A): ZipList<A> {
    return when (this) {
        is ZipListImpl -> ziplist(
            head = t(head),
            tail = tail
        )
    }
}

fun <A> ZipList<A>.head(): A {
    return when (this) {
        is ZipListImpl -> head
    }
}

fun <A> ZipList<A>.tail(): List<A> {
    return when (this) {
        is ZipListImpl -> tail
    }
}

