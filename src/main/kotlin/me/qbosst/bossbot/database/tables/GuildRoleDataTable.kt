package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object GuildRoleDataTable: Table()
{
    val guild_id = long("GUILD_ID").default(0L)
    val role_id = long("ROLE_ID").default(0L)
    val type = integer("TYPE").default(-1)

    enum class Type
    {
        AUTO_ROLE
    }


}