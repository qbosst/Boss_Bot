package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table
import java.time.ZoneId

object UserDataTable: Table() {

    private val MAX_ZONE_ID_LENGTH = ZoneId.getAvailableZoneIds().maxOf { it.length }

    val userId = long("user_id")
    val zoneId = varchar("zone_id", MAX_ZONE_ID_LENGTH).nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(userId)
    override val tableName: String = "user_data"
}