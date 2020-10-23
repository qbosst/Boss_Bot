package me.qbosst.bossbot.entities.database

import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.ZoneId

data class UserData private constructor(
        val greeting: String? = null,
        val zone_id: ZoneId? = null
)
{
    companion object
    {
        private val cache = FixedCache<Long, UserData>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())
        private val EMPTY = UserData()

        fun get(user: User): UserData
        {
            if(cache.contains(user.idLong))
            {
                return cache.get(user.idLong)!!
            }
            else
            {
                val data = transaction {
                    UserDataTable
                            .select { UserDataTable.user_id.eq(user.idLong) }
                            .fetchSize(1)
                            .map {
                                UserData(
                                        greeting = it[UserDataTable.greeting],
                                        zone_id = try { ZoneId.of(it[UserDataTable.zone_id]) } catch (t: Throwable) { null }
                                )
                            }
                            .singleOrNull()
                } ?: EMPTY
                cache.put(user.idLong, data)
                return data
            }
        }

        fun <T> update(user: User, column: Column<T?>, value: T?): T?
        {
            cache.pull(user.idLong)
            return transaction {
                val old = UserDataTable
                        .slice(column)
                        .select { UserDataTable.user_id.eq(user.idLong) }
                        .fetchSize(1)
                        .singleOrNull()

                if(old == null)
                {
                    UserDataTable
                            .insert {
                                it[user_id] = user.idLong
                                it[column] = value
                            }
                    return@transaction null
                }
                else
                {
                    UserDataTable
                            .update({ UserDataTable.user_id.eq(user.idLong) })
                            {
                                it[column] = value
                            }
                    return@transaction old[column]
                }
            }
        }
    }
}