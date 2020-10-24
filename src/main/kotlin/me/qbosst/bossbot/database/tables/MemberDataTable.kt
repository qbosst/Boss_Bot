package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object MemberDataTable : Table()
{
    val guild_id = long("GUILD_ID")
    val user_id = long("USER_ID")

    val experience = integer("EXPERIENCE").default(0)
    val message_count = integer("MESSAGE_COUNT").default(0)
    val text_chat_time = long("TEXT_CHAT_TIME").default(0L)
    val voice_chat_time = long("VOICE_CHAT_TIME").default(0L)

    override val primaryKey
        get() = PrimaryKey(guild_id, user_id)

    override val tableName
        get() = "MEMBER_DATA_TABLE"
}