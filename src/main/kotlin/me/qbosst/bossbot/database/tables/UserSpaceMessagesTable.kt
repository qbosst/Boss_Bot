package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object UserSpaceMessagesTable: Table() {

    val userId = long("user_id")
    val messageId = long("message_id")

    override val primaryKey: PrimaryKey = PrimaryKey(userId, messageId)
    override val tableName: String = "user_space_messages"
}