package me.qbosst.bossbot.database

import me.qbosst.bossbot.database.tables.InitTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseManager(
    host: String,
    username: String,
    password: String,
    val tables: List<Table>
) {
    val db: Database = Database.connect(
        url = "jdbc:mysql://$host",
        user = username,
        password = password
    )

    fun init() {
        transaction(db) {
            tables.forEach { table ->
                SchemaUtils.createMissingTablesAndColumns(table)

                if(table is InitTable) {
                    table.init()
                }
            }
        }
    }

    class Builder {
        lateinit var host: String
        lateinit var username: String
        lateinit var password: String
        val tables: MutableList<Table> = mutableListOf()

        fun build(): DatabaseManager = DatabaseManager(host, username, password, tables)

        fun tables(init: MutableList<Table>.() -> Unit) = tables.init()
    }
}

fun DatabaseManager(init: DatabaseManager.Builder.() -> Unit): DatabaseManager =
    DatabaseManager.Builder().apply(init).build()