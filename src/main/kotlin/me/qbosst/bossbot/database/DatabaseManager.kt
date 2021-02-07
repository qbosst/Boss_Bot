package me.qbosst.bossbot.database

import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.database.tables.UserDataTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseManager(
    private val host: String,
    private val username: String,
    private val password: String,
    val tables: List<Table>
) {
    lateinit var database: Database
        private set

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

    companion object {
        operator fun invoke(init: DatabaseManagerBuilder.() -> Unit) = DatabaseManagerBuilder().apply(init).build()
    }
}

class DatabaseManagerBuilder {
    lateinit var host: String
    lateinit var username: String
    lateinit var password: String
    var tables: MutableList<Table> = mutableListOf()

    fun tables(init: MutableList<Table>.() -> Unit) {
        this.tables = mutableListOf<Table>().apply(init)
    }

    fun build(): DatabaseManager = DatabaseManager(host, username, password, tables)
}