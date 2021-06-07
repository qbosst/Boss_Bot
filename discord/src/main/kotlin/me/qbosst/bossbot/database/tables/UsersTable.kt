package me.qbosst.bossbot.database.tables

import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction

object UsersTable: LongIdTable(name = "user_data", columnName = "user_id"), InitTable {

    val tokens = long("tokens").default(0)
    val timeZone = varchar("time_zone_id", getMaxLengthZoneId()).nullable()

    override fun init(transaction: Transaction): Unit = transaction.run {
        // save space by clearing records that only store default values
        deleteWhere {
            (tokens eq tokens.defaultValueFun!!.invoke())
                .and(timeZone eq timeZone.defaultValueFun?.invoke())
        }
    }

    private fun getMaxLengthZoneId(): Int = TimeZone.availableZoneIds.maxByOrNull { it.length }!!.length
}