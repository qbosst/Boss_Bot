package me.qbosst.bossbot.database.models

import dev.kord.cache.api.DataCache
import dev.kord.cache.api.data.description
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.cache.api.remove
import dev.kord.core.behavior.GuildBehavior
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class GuildSettings(
    val guildId: Long,
    val messageLogsChannelId: Long? = null,
    val prefix: String? = null
) {

    private operator fun <T> get(column: Column<T>): T = when(column) {
        GuildSettingsTable.guildId -> guildId
        GuildSettingsTable.messageLogsChannelId -> messageLogsChannelId
        GuildSettingsTable.prefix -> prefix
        else -> throw IllegalArgumentException("This column does not a corresponding attribute!")
    } as T

    companion object {
        val description = description(GuildSettings::guildId)

        fun retrieve(id: Long) = transaction { retrieve(id, this) }

        fun retrieve(id: Long, transaction: Transaction): GuildSettings? = transaction.run {
            GuildSettingsTable
                .select { GuildSettingsTable.guildId.eq(id) }
                .singleOrNull()?.let { row ->
                    GuildSettings(
                        guildId = row[GuildSettingsTable.guildId],
                        messageLogsChannelId = row[GuildSettingsTable.messageLogsChannelId],
                        prefix = row[GuildSettingsTable.prefix]
                    )
                }
        }

        suspend fun get(id: Long, cache: DataCache): GuildSettings? = cache.query<GuildSettings> { GuildSettings::guildId.eq(id) }.singleOrNull()

        suspend fun <T> update(id: Long, cache: DataCache, column: Column<T>, new: T): Pair<Boolean, T?> = newSuspendedTransaction {
            val record = get(id, cache) ?: retrieve(id, this)
            val old = record?.get(column)

            kotlin.run {
                when {
                    record == null -> {
                        GuildSettingsTable.insert {
                            it[GuildSettingsTable.guildId] = id
                            it[column] = new
                        }
                    }
                    new == old -> return@run

                    else -> {
                        GuildSettingsTable.update(
                            where = { GuildSettingsTable.guildId.eq(id) },
                            body = {
                                it[column] = new
                            }
                        )
                    }
                }
                cache.remove<GuildSettings> { GuildSettings::guildId.eq(id) }
            }

            return@newSuspendedTransaction (record != null) to old
        }

    }
}

suspend fun GuildBehavior.getSettings(): GuildSettings? = GuildSettings.get(id.value, kord.cache)

suspend fun GuildBehavior.retrieveSettings(cache: Boolean = true): GuildSettings = (GuildSettings.retrieve(id.value) ?: GuildSettings(id.value))
    .also { settings ->
        if(cache) {
            kord.cache.put(settings)
        }
    }

suspend fun GuildBehavior.getOrRetrieveSettings(): GuildSettings = getSettings() ?: retrieveSettings()