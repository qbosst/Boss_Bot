package me.qbosst.bossbot.database.models

import dev.kord.cache.api.data.description

class GuildSettings(
    val guildId: Long,
    val messageLogsChannelId: Long = 0L,
    val prefix: String? = null
) {
    companion object {
        val description = description(GuildSettings::guildId)
    }
}