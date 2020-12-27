package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table
import java.time.ZoneId

object UserDataTable : Table()
{
    const val MAX_GREETING_LENGTH = 512
    private val MAX_ZONE_ID_LENGTH = ZoneId.getAvailableZoneIds().maxOf { it.length }

    val userId = long("USER_ID")
    val greeting = varchar("GREETING", MAX_GREETING_LENGTH).nullable()
    val zoneId = varchar("ZONE_ID", MAX_ZONE_ID_LENGTH).nullable()

    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(userId)

    override val tableName: String
        get() = "USER_DATA"

}