package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object GuildSettingsTable: LongIdTable(columnName = "guild_id") {

    const val MAX_PREFIX_LENGTH = 4

    val messageLogsChannelId = long("message_logs_channel_id").nullable()

    val prefix = varchar("prefix", MAX_PREFIX_LENGTH).nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(id)
    override val tableName: String = "guild_settings"
}