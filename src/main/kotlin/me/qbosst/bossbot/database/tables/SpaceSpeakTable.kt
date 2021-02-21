package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object SpaceSpeakTable: LongIdTable(name = "spacespeak", columnName = "message_id") {
    val userId = long("user_id").nullable()
    val isAnonymous = bool("is_anonymous").default(false)
    val isPublic = bool("is_public").default(true)
}