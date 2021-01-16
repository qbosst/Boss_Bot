package me.qbosst.bossbot.database

import me.qbosst.bossbot.database.tables.GuildColoursTable
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

    val tables = listOf(GuildColoursTable)

    fun connect() {
        require(this::database.isInitialized.not()) { "Database is already initialized!" }

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
}