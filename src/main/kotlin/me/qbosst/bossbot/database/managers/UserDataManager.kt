package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.util.getZoneId
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.ZoneId

object UserDataManager: Manager<Long, UserDataManager.UserData>()
{

    private val EMPTY = UserData()

    override fun getDatabase(key: Long): UserData
    {
        return transaction {
            UserDataTable
                    .select { UserDataTable.user_id.eq(key) }
                    .fetchSize(1)
                    .map { row ->
                        UserData(
                                greeting = row[UserDataTable.greeting],
                                zone_id = getZoneId(row[UserDataTable.zone_id])
                        )
                    }
                    .singleOrNull() ?: EMPTY
        }
    }

    fun get(user: User) = UserDataManager.get(user.idLong)

    fun <T> update(user: User, column: Column<T>, value: T): T?
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

    data class UserData(
            val greeting: String? = null,
            val zone_id: ZoneId? = null
    )
}

fun User.getUserData(): UserDataManager.UserData = UserDataManager.get(this)