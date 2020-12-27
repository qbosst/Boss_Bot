package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table
import java.time.ZoneId

object GuildSettingsTable : Table()
{
    const val max_prefix_length = 8

    val guild_id = long("GUILD_ID")

    val suggestion_channel_id = long("SUGGESTION_CHANNEL_ID").default(0L)
    val message_logs_channel_id = long("MESSAGE_LOGS_CHANNEL_ID").default(0L)
    val voice_logs_channel_id = long("VOICE_LOGS_CHANNEL_ID").default(0L)

    val prefix = varchar("PREFIX", max_prefix_length).nullable()

    override val primaryKey
        get() = PrimaryKey(guild_id)

    override val tableName
        get() = "GUILD_SETTINGS_DATA"
}