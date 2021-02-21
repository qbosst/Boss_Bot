package me.qbosst.bossbot.database.dao

import dev.kord.cache.api.data.description
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.cache.data.GuildData
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Live entity containing settings for a [dev.kord.core.entity.Guild]
 */
class GuildSettings(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<GuildSettings>(GuildSettingsTable) {
        val description = description(GuildSettings::guildId) {
            link(GuildSettings::guildId to GuildData::id)
        }
    }

    val guildId get() = id.value
    var messageLogsChannelId by GuildSettingsTable.messageLogsChannelId
    var prefix by GuildSettingsTable.prefix

    override fun toString(): String = "GuildSettings(guildId=${guildId},prefix=${prefix})"
}

suspend fun GuildBehavior.getSettings(transaction: Transaction): GuildSettings {
    val idLong = id.value
    // check cache first before querying database for settings
    return kord.cache.query<GuildSettings> { GuildSettings::guildId eq idLong }.singleOrNull()
        ?: transaction
            .run { GuildSettings.findById(idLong) ?: GuildSettings.new(idLong) {} }
            .also { settings -> kord.cache.put(settings)  }
}

suspend fun GuildBehavior.getSettings(): GuildSettings = newSuspendedTransaction { getSettings(this) }

fun GuildSettings?.insertOrUpdate(
    transaction: Transaction,
    id: Long,
    builder: GuildSettings.() -> Unit
) = transaction.run { this@insertOrUpdate?.apply(builder) ?: GuildSettings.new(id, builder) }

fun GuildSettings?.insertOrUpdate(
    id: Long,
    builder: GuildSettings.() -> Unit
) = transaction { insertOrUpdate(this, id, builder) }



