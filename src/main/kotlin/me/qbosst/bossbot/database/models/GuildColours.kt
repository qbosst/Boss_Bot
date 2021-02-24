package me.qbosst.bossbot.database.models

import dev.kord.cache.api.data.description
import me.qbosst.bossbot.util.Colour

class GuildColours(val guildId: Long, val colours: Map<String, Colour>) {
    companion object {
        val description = description(GuildColours::guildId)
    }
}