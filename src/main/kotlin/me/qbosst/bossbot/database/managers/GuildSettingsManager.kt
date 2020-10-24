package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.util.getZoneId
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId

object GuildSettingsManager: Manager<Long, GuildSettingsManager.GuildSettings>()
{
    private val EMPTY = GuildSettings()

    override fun getDatabase(key: Long): GuildSettings
    {
        return transaction {
            GuildSettingsTable
                    .select { GuildSettingsTable.guild_id.eq(key) }
                    .fetchSize(1)
                    .map { row ->
                        GuildSettings(
                                suggestion_channel_id = row[GuildSettingsTable.suggestion_channel_id],
                                message_logs_channel_id = row[GuildSettingsTable.message_logs_channel_id],
                                voice_logs_channel_id = row[GuildSettingsTable.voice_logs_channel_id],
                                dj_role_id = row[GuildSettingsTable.dj_role_id],
                                zone_id = getZoneId(row[GuildSettingsTable.zone_id]),
                                prefix = row[GuildSettingsTable.prefix]
                        )
                    }
                    .singleOrNull() ?: EMPTY

        }
    }

    fun get(guild: Guild?) = get(guild?.idLong ?: -1)

    /**
     *  Updates a setting in the guild
     *
     *  @param guild The guild to update the setting of
     *  @param column The column to update
     *  @param value The new value
     *
     *  @return The old value of the setting
     */
    fun <T> update(guild: Guild, column: Column<T>, value: T): T?
    {
        pull(guild.idLong)
        return transaction {
            val old = GuildSettingsTable
                    .slice(column)
                    .select { GuildSettingsTable.guild_id.eq(guild.idLong) }
                    .fetchSize(1)
                    .singleOrNull()

            if(old == null)
                GuildSettingsTable.insert {
                    it[guild_id] = guild.idLong
                    it[column] = value
                }
            else
                GuildSettingsTable.update({ GuildSettingsTable.guild_id.eq(guild.idLong )}) {
                    it[column] = value
                }

            return@transaction old?.getOrNull(column)
        }
    }

    fun clear(guild: Guild)
    {
        transaction {
            GuildSettingsTable.deleteWhere { GuildSettingsTable.guild_id.eq(guild.idLong) }
        }
        pull(guild.idLong)
    }

    data class GuildSettings(
            private val suggestion_channel_id: Long = 0L,
            private val message_logs_channel_id: Long = 0L,
            private val voice_logs_channel_id: Long = 0L,

            private val dj_role_id: Long = 0L,

            val zone_id: ZoneId? = null,
            val prefix: String? = null
    )
    {
        fun getSuggestionChannel(guild: Guild): TextChannel? = guild.getTextChannelById(suggestion_channel_id)

        fun getMessageLogsChannel(guild: Guild): TextChannel? = guild.getTextChannelById(message_logs_channel_id)

        fun getVoiceLogsChannel(guild: Guild): TextChannel? = guild.getTextChannelById(voice_logs_channel_id)

        fun getDjRole(guild: Guild): Role? = guild.getRoleById(dj_role_id)
    }
}

fun Guild.getSettings(): GuildSettingsManager.GuildSettings = GuildSettingsManager.get(this)