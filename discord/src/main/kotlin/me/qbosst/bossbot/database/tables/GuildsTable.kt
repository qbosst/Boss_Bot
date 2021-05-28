package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction

object GuildsTable: LongIdTable(name = "guild_data", columnName = "guild_id"), InitTable {
    const val PREFIX_LENGTH = 8

    val prefix = varchar("prefix", PREFIX_LENGTH).nullable()
    val messageLogChannel = long("message_log_channel").nullable()

    override fun init() {
        // save space by clearing records that only store default values
        transaction {
            deleteWhere {
                (prefix eq prefix.defaultValueFun?.invoke())
                    .and(messageLogChannel eq messageLogChannel.defaultValueFun?.invoke())
            }
        }
    }
}