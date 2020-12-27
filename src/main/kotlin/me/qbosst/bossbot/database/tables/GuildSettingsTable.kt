package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object GuildSettingsTable : Table()
{
    const val MAX_PREFIX_LENGTH = 8

    val guild_id = long("GUILD_ID")

    val suggestionChannelId = long("SUGGESTION_CHANNEL_ID").default(0L)
    val messageLogsChannelId = long("MESSAGE_LOGS_CHANNEL_ID").default(0L)
    val voiceLogsChannelId = long("VOICE_LOGS_CHANNEL_ID").default(0L)

    val prefix = varchar("PREFIX", MAX_PREFIX_LENGTH).nullable()

    override val primaryKey
        get() = PrimaryKey(guild_id)

    override val tableName
        get() = "GUILD_SETTINGS_DATA"
}