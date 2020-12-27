package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object MemberDataTable : Table()
{
    val guildId = long("GUILD_ID")
    val userId = long("USER_ID")

    val experience = integer("EXPERIENCE").default(0)
    val messageCount = integer("MESSAGE_COUNT").default(0)
    val textChatTime = long("TEXT_CHAT_TIME").default(0L)
    val voiceChatTime = long("VOICE_CHAT_TIME").default(0L)

    override val primaryKey
        get() = PrimaryKey(guildId, userId)

    override val tableName
        get() = "MEMBER_DATA_TABLE"
}