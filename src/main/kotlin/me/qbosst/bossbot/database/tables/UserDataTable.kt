package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import java.time.ZoneId

object UserDataTable: LongIdTable(name = "user_data", columnName = "user_id") {
    private val MAX_ZONE_ID_LENGTH = ZoneId.getAvailableZoneIds().maxOf { id -> id.length }

    val zoneId = varchar("zone_id", MAX_ZONE_ID_LENGTH).nullable()
}