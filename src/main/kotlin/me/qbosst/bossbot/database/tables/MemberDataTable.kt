package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object MemberDataTable: Table() {

    val guildId = long("guild_id")
    val userId = long("user_id")

    val experience = integer("experience").default(0)
    val messageCount = integer("message_count").default(0)
    val textChatTime = long("text_chat_time").default(0L)
    val voiceChatTime = long("voice_chat_time").default(0L)

    override val primaryKey: PrimaryKey = PrimaryKey(guildId, userId)
    override val tableName: String = "member_data"
}