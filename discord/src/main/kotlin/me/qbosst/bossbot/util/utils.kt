package me.qbosst.bossbot.util

import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.common.Color
import dev.kord.core.cache.Generator
import dev.kord.core.cache.KordCacheBuilder
import dev.kord.core.entity.Entity
import dev.kord.core.entity.Member
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.flow.firstOrNull

val Entity.idLong: Long get() = id.value

val Color.Companion.DEFAULT_ROLE_COLOUR: Color get() = Color(0x000000)

fun <K, V: Any> KordCacheBuilder.mapLikeCollection(
    map: MapLikeCollection<K, V>
): Generator<K, V> = { cache, description ->
    MapEntryCache(cache, description, map)
}

fun String.zeroWidthIfBlank() = ifBlank { "\u200E" }

suspend fun Member.getColour(): Color? = roles.firstOrNull { it.color != Color.DEFAULT_ROLE_COLOUR }?.color