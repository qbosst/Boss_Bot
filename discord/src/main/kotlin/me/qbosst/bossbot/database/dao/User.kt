package me.qbosst.bossbot.database.dao

import dev.kord.cache.api.data.description
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.cache.data.UserData
import me.qbosst.bossbot.database.tables.UsersTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class User(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<User>(UsersTable) {
        val description = description(User::userId) {
            link(User::userId to UserData::id)
        }
    }

    val userId: Long get() = id.value
    var tokens: Long by UsersTable.tokens
}

fun User?.insertOrUpdate(
    transaction: Transaction,
    id: Long,
    builder: User.() -> Unit
) = transaction.run { this@insertOrUpdate?.apply(builder) ?: User.new(id, builder) }

fun User?.insertOrUpdate(
    id: Long,
    builder: User.() -> Unit
) = transaction { insertOrUpdate(this, id, builder) }

suspend fun UserBehavior.getUserDAO(transaction: Transaction): User {
    val idLong = id.value

    return kord.cache.query<User> { User::userId eq idLong }.singleOrNull()
        ?: transaction
            .run { User.findById(idLong) ?: User.new(idLong) {} }
            .also { user -> kord.cache.put(user) }
}

suspend fun UserBehavior.getUserDAO(): User = newSuspendedTransaction { getUserDAO(this) }