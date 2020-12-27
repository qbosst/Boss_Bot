package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.entities.database.upsert
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId

object GuildSettingsManager: TableManager<Long, GuildSettingsManager.GuildSettings>()
{
    override fun retrieve(key: Long): GuildSettings = transaction {
        GuildSettingsTable
                .select { GuildSettingsTable.guild_id.eq(key) }
                .fetchSize(1)
                .map { row ->
                    GuildSettings(
                            suggestionChannelId = row[GuildSettingsTable.suggestion_channel_id],
                            messageLogsChannelId = row[GuildSettingsTable.message_logs_channel_id],
                            voiceLogsChannelId = row[GuildSettingsTable.voice_logs_channel_id],

                            prefix = row[GuildSettingsTable.prefix]
                    )
                }
                .singleOrNull() ?: GuildSettings()
    }

    fun get(guild: Guild?) = getOrRetrieve(guild?.idLong ?: -1)

    /**
     *  Updates a setting in the guild
     *
     *  @param guild The guild to update the setting of
     *  @param column The column to update
     *  @param value The new value
     *
     *  @return The old value of the setting
     */
    fun <T> update(guild: Guild, column: Column<T>, new: T): T? = transaction {
        val key = guild.idLong

        val old = if(isCached(key)) get(key)!![column] else GuildSettingsTable
                .slice(column)
                .select { GuildSettingsTable.guild_id.eq(key) }
                .fetchSize(1)
                .map { it[column] }
                .singleOrNull()

        if(old != new)
        {
            GuildSettingsTable.upsert(GuildSettingsTable.guild_id) {
                it[guild_id] = key
                it[column] = new
            }
            pull(key)
        }
        return@transaction old
    }

    fun clear(guild: Guild) = transaction {
        GuildSettingsTable
                .deleteWhere { GuildSettingsTable.guild_id.eq(guild.idLong) }
                .also { pull(guild.idLong) }
    }

    data class GuildSettings(private val suggestionChannelId: Long = 0L,
                             private val messageLogsChannelId: Long = 0L,
                             private val voiceLogsChannelId: Long = 0L,

                             val prefix: String? = null
    )
    {
        fun getSuggestionChannel(guild: Guild): TextChannel? = guild.getTextChannelById(suggestionChannelId)

        fun getMessageLogsChannel(guild: Guild): TextChannel? = guild.getTextChannelById(messageLogsChannelId)

        fun getVoiceLogsChannel(guild: Guild): TextChannel? = guild.getTextChannelById(voiceLogsChannelId)

        @Suppress("UNCHECKED_CAST")
        operator fun <T> get(column: Column<T>): T = when(column)
        {
            GuildSettingsTable.prefix ->
                prefix
            GuildSettingsTable.message_logs_channel_id ->
                messageLogsChannelId
            GuildSettingsTable.voice_logs_channel_id ->
                voiceLogsChannelId
            GuildSettingsTable.suggestion_channel_id ->
                suggestionChannelId
            else ->
                throw UnsupportedOperationException("This column does not have a corresponding attribute!")
        } as T
    }
}

fun Guild.getSettings(): GuildSettingsManager.GuildSettings = GuildSettingsManager.get(this)