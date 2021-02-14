package me.qbosst.bossbot.database.models

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

class UserData(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<UserData>(UserDataTable) {
        val description = description(UserData::userId)
    }

    val userId: Long get() = id.value
    var zoneId by UserDataTable.zoneId

    override fun toString(): String = "UserData(" +
            "zoneId=${zoneId})"
}

suspend fun UserBehavior.retrieveUserData(transaction: Transaction): UserData? {
    val idLong = id.value
    val data = transaction.run {
        UserData.findById(idLong)
    }

    if(data != null) {
        kord.cache.put(data)
    }

    return data
}

suspend fun UserBehavior.retrieveUserData() = newSuspendedTransaction { retrieveUserData(this) }

suspend fun UserBehavior.getUserData() = kord.cache.query<UserData> { UserData::userId eq id.value }.singleOrNull()

suspend fun UserBehavior.getOrRetrieveUserData(transaction: Transaction) = getUserData() ?: retrieveUserData(transaction)

suspend fun UserBehavior.getOrRetrieveUserData() = getUserData() ?: newSuspendedTransaction { retrieveUserData(this) }