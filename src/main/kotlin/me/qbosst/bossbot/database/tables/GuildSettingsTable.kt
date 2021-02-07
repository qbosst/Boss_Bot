package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object GuildSettingsTable: Table() {

    const val MAX_PREFIX_LENGTH = 4

    val guildId = long("guild_id")

    val messageLogsChannelId = long("message_logs_channel_id").nullable()

    val prefix = varchar("prefix", MAX_PREFIX_LENGTH).nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(guildId)
    override val tableName: String = "guild_settings"
}