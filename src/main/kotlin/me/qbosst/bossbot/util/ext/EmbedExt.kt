package me.qbosst.bossbot.util.ext

import dev.kord.common.entity.optional.Optional
import dev.kord.core.cache.data.EmbedData
import dev.kord.rest.json.request.EmbedRequest
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
fun EmbedRequest.isEmpty(): Boolean {
    val properties = (this::class.memberProperties as Collection<KProperty1<EmbedRequest, Optional<*>>>)

    return properties.none { prop -> prop.get(this).value != null }
}