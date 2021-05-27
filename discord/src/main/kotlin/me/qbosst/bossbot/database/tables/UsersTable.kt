package me.qbosst.bossbot.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction

object UsersTable: LongIdTable(name = "user_data", columnName = "user_id"), InitTable {
    val tokens = long("tokens").default(0)

    override fun init() {
        // save space by clearing records that only store default values
        transaction {
            deleteWhere {
                (tokens eq tokens.defaultValueFun!!.invoke())
            }
        }
    }
}