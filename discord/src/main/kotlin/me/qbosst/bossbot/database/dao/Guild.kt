package me.qbosst.bossbot.database.dao

import dev.kord.cache.api.data.description
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.cache.data.GuildData
import me.qbosst.bossbot.database.tables.GuildsTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class Guild(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<Guild>(GuildsTable) {
        val description = description(Guild::guildId) {
            link(Guild::guildId to GuildData::id)
        }
    }

    val guildId: Long get() = id.value
    var prefix: String? by GuildsTable.prefix
}

fun Guild?.insertOrUpdate(
    transaction: Transaction,
    id: Long,
    builder: Guild.() -> Unit
) = transaction.run { this@insertOrUpdate?.apply(builder) ?: Guild.new(id, builder) }

fun Guild?.insertOrUpdate(
    id: Long,
    builder: Guild.() -> Unit
) = transaction { insertOrUpdate(this, id, builder) }

suspend fun GuildBehavior.getGuildDAO(transaction: Transaction): Guild {
    val idLong = id.value

    return kord.cache.query<Guild> { Guild::guildId eq idLong }.singleOrNull()
        ?: transaction
            .run { Guild.findById(idLong) ?: Guild.new(idLong) {} }
            .also { user -> kord.cache.put(user) }
}

suspend fun GuildBehavior.getGuildDAO(): Guild = newSuspendedTransaction { getGuildDAO(this) }