package me.qbosst.bossbot.database.manager

import me.qbosst.bossbot.database.tables.GuildSettingsTable
import net.dv8tion.jda.api.entities.Guild
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object GuildSettingsManager: TableManager<Long, GuildSettings>() {

    override fun retrieve(key: Long): GuildSettings = transaction {
        return@transaction GuildSettingsTable
            .select { GuildSettingsTable.guildId.eq(key) }
            .singleOrNull()
            ?.let { row ->
                GuildSettings(
                    row[GuildSettingsTable.guildId],
                    row[GuildSettingsTable.suggestionChannelId],
                    row[GuildSettingsTable.messageLogsChannelId],
                    row[GuildSettingsTable.prefix]
                )
            }
            ?: GuildSettings(key)
    }

    override fun delete(key: Long) {
        transaction {
            GuildSettingsTable.deleteWhere { GuildSettingsTable.guildId.eq(key) }
            pull(key)
        }
    }

    fun <T> update(key: Long, column: Column<T>, value: T): T? = transaction {
        pull(key)
        val old = GuildSettingsTable
            .slice(column)
            .select { GuildSettingsTable.guildId.eq(key) }
            .singleOrNull()

        if(old == null) {
            GuildSettingsTable.insert {
                it[guildId] = key
                it[column] = value
            }
        }
        else {
            GuildSettingsTable.update ({ GuildSettingsTable.guildId.eq(key) }) {
                it[column] = value
            }
        }

        return@transaction old?.getOrNull(column)
    }
}

data class GuildSettings(
    val guildId: Long,
    val suggestionChannelId: Long = 0L,
    val messageLogsChannelId: Long = 0L,
    val prefix: String? = null
)

val Guild.settings: GuildSettings
    get() = GuildSettingsManager.getOrRetrieve(this.idLong)

