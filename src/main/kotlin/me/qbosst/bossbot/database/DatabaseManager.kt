package me.qbosst.bossbot.database

import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.database.tables.UserDataTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseManager(
    private val host: String,
    private val username: String,
    private val password: String
) {

    lateinit var database: Database
        private set

    val tables = listOf(GuildColoursTable, UserDataTable)

    fun connect() {
        require(!this::database.isInitialized) { "Database is already initialized!" }

        database = Database.connect(
            url = "jdbc:mysql://$host",
            user = username,
            password = password
        )

        transaction {
            tables.forEach { table ->
                SchemaUtils.createMissingTablesAndColumns(table)
            }
        }
    }

    class Builder {
        lateinit var host: String
        lateinit var username: String
        lateinit var password: String

        fun build(): DatabaseManager = DatabaseManager(host, username, password)
    }
}