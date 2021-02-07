package me.qbosst.bossbot.database.models

import dev.kord.cache.api.data.description
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import org.jetbrains.exposed.sql.Column

class GuildSettings(
    val guildId: Long,
    val messageLogsChannelId: Long? = null,
    val prefix: String? = null
) {

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(column: Column<T>): T = when(column) {
        GuildSettingsTable.guildId -> guildId as T

        GuildSettingsTable.messageLogsChannelId -> messageLogsChannelId as T

        GuildSettingsTable.prefix -> prefix as T

        else -> throw IllegalArgumentException("This column does not have a corresponding value!")
    }

    companion object {
        val description = description(GuildSettings::guildId)
    }
}