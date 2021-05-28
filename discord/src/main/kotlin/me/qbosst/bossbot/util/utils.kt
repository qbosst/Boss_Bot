package me.qbosst.bossbot.util

import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.core.cache.Generator
import dev.kord.core.cache.KordCacheBuilder
import dev.kord.core.entity.Entity

val Entity.idLong: Long get() = id.value

fun <K, V: Any> KordCacheBuilder.mapLikeCollection(
    map: MapLikeCollection<K, V>
): Generator<K, V> = { cache, description ->
    MapEntryCache(cache, description, map)
}