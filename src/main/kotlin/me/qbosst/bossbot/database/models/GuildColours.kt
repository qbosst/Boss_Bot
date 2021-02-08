package me.qbosst.bossbot.database.models

import dev.kord.cache.api.data.description
import java.awt.Color as Colour

class GuildColours(val guildId: Long, val data: Map<String, Colour>) {
    companion object {
        val description = description(GuildColours::guildId)
    }
}