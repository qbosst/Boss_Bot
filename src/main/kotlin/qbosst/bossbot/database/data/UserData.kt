package qbosst.bossbot.database.data

import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import qbosst.bossbot.config.Config
import qbosst.bossbot.database.tables.GuildSettingsDataTable
import qbosst.bossbot.database.tables.GuildUserDataTable
import qbosst.bossbot.database.tables.UserDataTable
import qbosst.bossbot.util.FixedCache
import java.time.Instant

data class UserData private constructor(
        val greeting: String? = null
)
{
    companion object
    {
        private val cache = FixedCache<Long, UserData>(Config.Values.DEFAULT_CACHE_SIZE.getInt())
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
                                        greeting = it[UserDataTable.greeting]
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
                                it[UserDataTable.user_id] = user.idLong
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