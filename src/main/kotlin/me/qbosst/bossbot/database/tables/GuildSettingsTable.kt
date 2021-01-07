package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object GuildSettingsTable: Table() {

    const val MAX_PREFIX_LENGTH = 8

    val guildId = long("guild_id")

    val suggestionChannelId = long("suggestion_channel_id").default(0L)
    val messageLogsChannelId = long("message_logs_channel_id").default(0L)

    val prefix = varchar("prefix", MAX_PREFIX_LENGTH).nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(guildId)
    override val tableName: String = "guild_settings"
}