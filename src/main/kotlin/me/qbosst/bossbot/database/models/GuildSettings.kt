package me.qbosst.bossbot.database.models

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

class GuildSettings(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<GuildSettings>(GuildSettingsTable) {
        val description = description(GuildSettings::guildId) {
            link(GuildSettings::guildId to GuildData::id)
        }
    }

    val guildId: Long get() = id.value
    var messageLogsChannelId by GuildSettingsTable.messageLogsChannelId
    var prefix by GuildSettingsTable.prefix

    override fun toString(): String = "GuildSettings(" +
            "guildId=${guildId}, " +
            "messageLogsChannelId=${messageLogsChannelId}, " +
            "prefix=${prefix})"
}

/**
 * Retrieves a [dev.kord.core.entity.Guild]'s settings from the database
 *
 * @param transaction The transaction to use to retrieve the settings
 *
 * @return the settings of the [dev.kord.core.entity.Guild], or null if the guild has no settings.
 */
suspend fun GuildBehavior.retrieveSettings(transaction: Transaction): GuildSettings? {
    val idLong = id.value
    val settings = transaction.run {
        GuildSettings.findById(idLong)
    }

    if(settings != null) {
        kord.cache.put(settings)
    }

    return settings
}

/**
 * Retrieves a [dev.kord.core.entity.Guild]'s settings from the database
 *
 * @return the settings of the [dev.kord.core.entity.Guild], or null if the guild has no settings.
 */
suspend fun GuildBehavior.retrieveSettings() = newSuspendedTransaction { retrieveSettings(this) }

/**
 * Gets a [dev.kord.core.entity.Guild]'s settings from kord's cache
 *
 * @return the settings of the [dev.kord.core.entity.Guild], or null if the settings are not cached
 */
suspend fun GuildBehavior.getSettings() = kord.cache.query<GuildSettings> { GuildSettings::guildId eq id.value }.singleOrNull()

/**
 * Gets a [dev.kord.core.entity.Guild]'s settings by first checking kord's cache and then retrieving it from the database
 *
 * @param transaction The transaction to use to retrieve the settings from the database
 *
 * @return the settings of the [dev.kord.core.entity.Guild], or null if the guild has no settings.
 */
suspend fun GuildBehavior.getOrRetrieveSettings(transaction: Transaction) = getSettings() ?: retrieveSettings(transaction)

/**
 * Gets a [dev.kord.core.entity.Guild]'s settings by first checking kord's cache and then retrieving it from the database
 *
 * @return the settings of the [dev.kord.core.entity.Guild], or null if the guild has no settings.
 */
suspend fun GuildBehavior.getOrRetrieveSettings() = getSettings() ?: newSuspendedTransaction { retrieveSettings(this) }
