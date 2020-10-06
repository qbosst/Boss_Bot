package me.qbosst.bossbot.database

import me.qbosst.bossbot.util.loadObjects
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object Database
{
    private val tables: Collection<Table>
        get() = loadObjects("${this::class.java.`package`.name}.tables", Table::class.java)

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

        // Creates missing tables and columns
        transaction()
        {
            for(table in tables)
                SchemaUtils.createMissingTablesAndColumns(table)
        }
    }
}