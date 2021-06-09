package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object SpaceSpeakTable: LongIdTable(name = "spacespeak", columnName = "message_id") {
    val userId = long("user_id").nullable()
}