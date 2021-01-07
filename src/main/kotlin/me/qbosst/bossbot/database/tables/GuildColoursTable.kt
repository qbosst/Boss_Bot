package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object GuildColoursTable: Table() {

    const val MAX_COLOUR_NAME_LENGTH = 32

    val guildId = long("guild_id")
    val name = varchar("name", MAX_COLOUR_NAME_LENGTH)
    val value = integer("value")

    override val primaryKey: PrimaryKey = PrimaryKey(guildId, name)
    override val tableName: String = "guild_colours"
}