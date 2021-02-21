package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object GuildColoursTable: Table(name = "guild_colours") {
    const val MAX_NAME_LENGTH = 32

    val guildId = long("guild_id")
    val name = varchar("name", MAX_NAME_LENGTH)
    val value = integer("value")

    override val primaryKey: PrimaryKey = PrimaryKey(guildId, name)
}