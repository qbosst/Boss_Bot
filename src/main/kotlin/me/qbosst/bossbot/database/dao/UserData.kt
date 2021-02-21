package me.qbosst.bossbot.database.dao

import dev.kord.cache.api.data.description
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.core.behavior.UserBehavior
import me.qbosst.bossbot.database.tables.UserDataTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId

class UserData(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<UserData>(UserDataTable) {
        val description = description(UserData::userId) {
            link(UserData::userId to dev.kord.core.cache.data.UserData::id)
        }
    }

    val userId get() = id.value
    var zoneId: ZoneId? by UserDataTable.zoneId.transform({ zoneId?.id }, { it?.let { ZoneId.of(it) } })

    override fun toString(): String = "UserData(userId=${userId},zoneId=${zoneId})"
}

suspend fun UserBehavior.getUserData(transaction: Transaction): UserData {
    val idLong = id.value

    return kord.cache.query<UserData> { UserData::userId eq idLong }.singleOrNull()
        ?: transaction
            .run { UserData.findById(idLong) ?: UserData.new(idLong) {} }
            .also { data -> kord.cache.put(data) }
}

suspend fun UserBehavior.getUserData() = newSuspendedTransaction { getUserData(this) }

fun UserData?.insertOrUpdate(
    transaction: Transaction,
    id: Long,
    builder: UserData.() -> Unit
) = transaction.run { this@insertOrUpdate?.apply(builder) ?: UserData.new(id, builder) }

fun UserData?.insertOrUpdate(
    id: Long,
    builder: UserData.() -> Unit
) = transaction { insertOrUpdate(this, id, builder) }