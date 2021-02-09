package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object SpaceSpeakTable: Table() {

    val messageId = long("message_id")
    val userId = long("user_id")
    val isAnonymous = bool("is_anonymous").default(false)
    val isPublic = bool("is_public").default(true)

    override val tableName: String = "spacespeak"
    override val primaryKey: PrimaryKey = PrimaryKey(messageId, userId)
}