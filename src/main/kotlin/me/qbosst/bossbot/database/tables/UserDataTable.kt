package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import java.time.ZoneId

object UserDataTable: LongIdTable(name = "user_data", columnName = "user_id") {
    private val maxZoneIdLength = ZoneId.getAvailableZoneIds().maxOf { id -> id.length }

    val zoneId = varchar("zone_id", maxZoneIdLength).nullable()
}