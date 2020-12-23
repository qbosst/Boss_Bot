package me.qbosst.bossbot.database

import me.qbosst.bossbot.database.managers.TableManager
import me.qbosst.bossbot.util.loadObjects
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object Database
{
    private val tables: Collection<Table>
        get() = loadObjects("${this::class.java.`package`.name}.tables", Table::class.java)

    private val managers: Collection<TableManager<*, *>>
        get() = loadObjects("${this::class.java.`package`.name}.managers", TableManager::class.java)

    /**
     *  Connects to the database that the bot will be using to store data
     *
     *  @param host the host of the database
     *  @param user the username that the bot should connect with
     *  @param password the password that the bot should connect with
     */
    fun connect(host: String, user: String, password: String)
    {
        // Connects to the database
        Database.connect(
            url = "jdbc:mysql://$host?serverTimezone=UTC&useUnicode=true&wait_timeout=86400",
            user = user,
            password = password
        )

        transaction()
        {
            // Creates missing tables and columns
            for(table in tables)
                SchemaUtils.createMissingTablesAndColumns(table)
        }
        // Signal that the database is ready
        for(manager in managers)
            manager.onReady()
    }
}