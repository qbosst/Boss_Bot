package me.qbosst.bossbot.database.models

import dev.kord.cache.api.DataCache
import dev.kord.cache.api.data.description
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.database.tables.UserDataTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId

data class UserData(
    val userId: Long,
    val zoneId: ZoneId? = null
) {
    companion object {
        val description = description(UserData::userId)

        fun retrieve(id: Long) = transaction { retrieve(id, this) }

        fun retrieve(id: Long, transaction: Transaction): UserData? = transaction.run {
            UserDataTable
                .select { UserDataTable.userId.eq(id) }
                .singleOrNull()?.let { row ->
                    UserData(
                        userId = row[UserDataTable.userId],
                        zoneId = row[UserDataTable.zoneId]?.let { id -> ZoneId.of(id) }
                    )
                }
        }

        suspend fun get(id: Long, cache: DataCache): UserData? = cache.query<UserData> { UserData::userId.eq(id) }.singleOrNull()
    }
}

suspend fun UserBehavior.getUserData(): UserData? = UserData.get(id.value, kord.cache)

suspend fun UserBehavior.retrieveUserData(cache: Boolean = true): UserData = (UserData.retrieve(id.value) ?: UserData(id.value))
    .also { data ->
        if(cache) {
            kord.cache.put(data)
        }
    }

suspend fun UserBehavior.getOrRetrieveData(): UserData = getUserData() ?: retrieveUserData()