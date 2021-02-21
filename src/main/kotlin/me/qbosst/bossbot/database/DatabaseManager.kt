package me.qbosst.bossbot.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Manages the database
 */
class DatabaseManager(
    private val host: String,
    private val username: String,
    private val password: String,
    val tables: List<Table>
) {
    /**
     * The actual database instance
     */
    lateinit var db: Database private set

    /**
     * Creates the database instance
     */
    fun init() {
        require(!::db.isInitialized) { "Database is already initialized!" }

        // establish how we connect to the database
        db = Database.connect(
            url = "jdbc:mysql://$host",
            user = username,
            password = password
        )

        // create the tables in the database if missing
        transaction(db) {
            tables.forEach { table ->
                SchemaUtils.createMissingTablesAndColumns(table)
            }
        }
    }

    /**
     * DSL function for creating an instance of a [DatabaseManager]
     */
    class Builder {
        lateinit var host: String
        lateinit var username: String
        lateinit var password: String
        val tables: MutableList<Table> = mutableListOf()

        fun build(): DatabaseManager = DatabaseManager(host, username, password, tables)

        fun tables(init: MutableList<Table>.() -> Unit) {
            tables.apply(init)
        }
    }
}