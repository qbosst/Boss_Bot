package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.util.TimeUtil
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.ZoneId

object UserDataManager: TableManager<Long, UserDataManager.UserData>()
{
    override fun retrieve(key: Long): UserData = transaction {
        UserDataTable
                .select { UserDataTable.user_id.eq(key) }
                .fetchSize(1)
                .map { row ->
                    UserData(
                            greeting = row[UserDataTable.greeting],
                            zone_id = TimeUtil.zoneIdOf(row[UserDataTable.zone_id])
                    )
                }
                .singleOrNull() ?: UserData()
    }

    fun get(user: User) = UserDataManager.getOrRetrieve(user.idLong)

    fun <T> update(user: User, column: Column<T>, new: T): T? = transaction {
        val key = user.idLong
        val old = if(isCached(key)) get(key)!![column] else UserDataTable
                .slice(column)
                .select { UserDataTable.user_id.eq(key) }
                .fetchSize(1)
                .map { it[column] }
                .singleOrNull()

        if(old != new)
        {
            if(old == null)
                UserDataTable.insert {
                    it[user_id] = key
                    it[column] = new
                }
            else
                UserDataTable.update ({ UserDataTable.user_id.eq(key) }) {
                    it[column] = new
                }
            pull(key)
        }

        return@transaction old
    }
    /*
    {
        pull(user.idLong)
        return transaction {
            val old = UserDataTable
                    .slice(column)
                    .select { UserDataTable.user_id.eq(user.idLong) }
                    .fetchSize(1)
                    .singleOrNull()

            if(old == null)
                UserDataTable.insert {
                    it[user_id] = user.idLong
                    it[column] = value
                }

            else
                UserDataTable.update({ UserDataTable.user_id.eq(user.idLong) }) {
                    it[column] = value
                }

            return@transaction old?.getOrNull(column)
        }
    }

     */

    data class UserData(
            val greeting: String? = null,
            val zone_id: ZoneId? = null
    )
    {
        @Suppress("UNCHECKED_CAST")
        operator fun <T> get(column: Column<T>): T = when(column)
        {
            UserDataTable.greeting ->
                greeting
            UserDataTable.zone_id ->
                zone_id
            else ->
                throw UnsupportedOperationException("This column does not have a corresponding attribute!")
        } as T
    }
}

fun User.getUserData(): UserDataManager.UserData = UserDataManager.get(this)