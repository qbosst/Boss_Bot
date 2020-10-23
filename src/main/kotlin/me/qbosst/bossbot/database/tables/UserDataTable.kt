package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table
import java.time.ZoneId

object UserDataTable : Table() {
    const val max_greeting_length = 512
    val max_zone_id_length = ZoneId.getAvailableZoneIds().maxOf { it.length }

    val user_id = long("USER_ID").default(0L)
    val greeting = varchar("GREETING", max_greeting_length).nullable()
    val zone_id = varchar("ZONE_ID", max_zone_id_length).nullable()

    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(user_id)

    override val tableName: String
        get() = "USER_DATA"

}