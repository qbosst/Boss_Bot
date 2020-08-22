package qbosst.bossbot.database.tables

import org.jetbrains.exposed.sql.Table

object UserDataTable : Table() {
    const val max_greeting_length = 512

    val user_id = long("USER_ID").default(0L)
    val greeting = varchar("GREETING", max_greeting_length).nullable()

    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(user_id)

    override val tableName: String
        get() = "USER_DATA"

}