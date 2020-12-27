package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.entities.database.upsert
import me.qbosst.bossbot.util.TimeUtil
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId

object UserDataManager: TableManager<Long, UserDataManager.UserData>()
{
    override fun retrieve(key: Long): UserData = transaction {
        UserDataTable
                .select { UserDataTable.userId.eq(key) }
                .fetchSize(1)
                .map { row ->
                    UserData(
                            greeting = row[UserDataTable.greeting],
                            zone_id = TimeUtil.zoneIdOf(row[UserDataTable.zoneId])
                    )
                }
                .singleOrNull() ?: UserData()
    }

    fun get(user: User) = UserDataManager.getOrRetrieve(user.idLong)

    fun <T> update(user: User, column: Column<T>, new: T): T? = transaction {
        val key = user.idLong
        val old = if(isCached(key)) get(key)!![column] else UserDataTable
                .slice(column)
                .select { UserDataTable.userId.eq(key) }
                .fetchSize(1)
                .map { it[column] }
                .singleOrNull()

        if(old != new)
        {
            UserDataTable.upsert {
                it[userId] = key
                it[column] = new
            }
            pull(key)
        }

        return@transaction old
    }

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
            UserDataTable.zoneId ->
                zone_id?.id
            else ->
                throw UnsupportedOperationException("This column does not have a corresponding attribute!")
        } as T
    }
}

fun User.getUserData(): UserDataManager.UserData = UserDataManager.get(this)