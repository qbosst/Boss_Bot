package me.qbosst.bossbot.entities.database

import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.tables.GuildSettingsDataTable
import me.qbosst.bossbot.entities.JSONEmbedBuilder
import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.*
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.json.JSONException
import org.json.JSONObject
import java.time.DateTimeException
import java.time.ZoneId

data class GuildSettingsData private constructor(
        private val suggestion_channel_id: Long = 0L,
        private val message_logs_channel_id: Long = 0L,
        private val voice_logs_channel_id: Long = 0L,
        private val moderation_logs_channel_id: Long = 0L,
        private val welcome_channel_id: Long = 0L,

        private val mute_role_id: Long = 0L,
        private val dj_role_id: Long = 0L,

        val welcome_message: JSONObject? = null,
        val zone_id: ZoneId = ZoneId.systemDefault(),
        private val prefix: String? = null,

        val requireReasonForPunish: Boolean = true
)
{
    fun getPrefixOr(string: String): String {
        return prefix ?: if(string.isEmpty())
        {
            throw IllegalArgumentException("Prefixes cannot be blank!")
        }
        else
        {
            string
        }
    }

    fun getSuggestionChannel(guild: Guild): TextChannel?
    {
        return guild.getTextChannelById(suggestion_channel_id)
    }

    fun getMessageLogsChannel(guild: Guild): TextChannel?
    {
        return guild.getTextChannelById(message_logs_channel_id)
    }

    fun getVoiceLogsChannel(guild: Guild): TextChannel?
    {
        return guild.getTextChannelById(voice_logs_channel_id)
    }

    fun getModerationLogsChannel(guild: Guild): TextChannel?
    {
        return guild.getTextChannelById(moderation_logs_channel_id)
    }

    fun getWelcomeChannel(guild: Guild): TextChannel?
    {
        return guild.getTextChannelById(welcome_channel_id)
    }

    fun getWelcomeMessage(member: Member): Message
    {

        val builder = MessageBuilder()
        val content = welcome_message?.toString()
                ?.replace("%user_mention%", member.asMention)
                ?.replace("%user_name%", member.user.name)
                ?.replace("%user_discriminator%", member.user.discriminator)
                ?.replace("%user_id%", member.id)
                ?.replace("%guild_name%", member.guild.name)
                ?.replace("%guild_count%", member.guild.members.size.toString())
                ?.replace("%user_avatar_url%", member.user.effectiveAvatarUrl)
                ?.replace("%guild_icon_url%", member.guild.iconUrl ?: member.user.defaultAvatarUrl)
                ?.replace("%user_join%", member.timeJoined.toEpochSecond().toString())

        val json = try { JSONObject(content) } catch (e: JSONException) { null }

        if(json != null)
        {
            if(json.has("content"))
            {
                builder.setContent(json.get("content").toString())
            }

            if(json.has("embed"))
            {
                if(json.get("embed") is JSONObject)
                    builder.setEmbed(JSONEmbedBuilder(json.getJSONObject("embed")).build())
                else if(!json.get("embed").toString().isNullOrBlank())
                    throw IllegalStateException("Embed must be a JSONObject!")
            }
        }
        return builder.build()
    }



    fun getMuteRole(guild: Guild): Role?
    {
        return guild.getRoleById(mute_role_id)
    }

    fun getDjRole(guild: Guild): Role?
    {
        return guild.getRoleById(dj_role_id)
    }

    companion object
    {
        private val cache = FixedCache<Long, GuildSettingsData>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())
        private val EMPTY = GuildSettingsData()

        fun get(guild: Guild?): GuildSettingsData
        {
            if(guild == null)
            {
                return EMPTY
            }
            if(cache.contains(guild.idLong))
            {
                return cache.get(guild.idLong)!!
            }
            else
            {
                val data = transaction {
                    GuildSettingsDataTable
                            .select { GuildSettingsDataTable.guild_id.eq(guild.idLong) }
                            .fetchSize(1)
                            .map {
                                GuildSettingsData(
                                        suggestion_channel_id = it[GuildSettingsDataTable.suggestion_channel_id],
                                        message_logs_channel_id = it[GuildSettingsDataTable.message_logs_channel_id],
                                        voice_logs_channel_id = it[GuildSettingsDataTable.voice_logs_channel_id],
                                        moderation_logs_channel_id = it[GuildSettingsDataTable.moderation_logs_channel_id],
                                        welcome_channel_id = it[GuildSettingsDataTable.welcome_channel_id],

                                        mute_role_id = it[GuildSettingsDataTable.mute_role_id],
                                        dj_role_id = it[GuildSettingsDataTable.dj_role_id],

                                        welcome_message = kotlin.run {
                                            val content = it[GuildSettingsDataTable.welcome_message]
                                            if(content == null)
                                                null
                                            else try
                                                {
                                                    JSONObject(content)
                                                } catch (e: JSONException)
                                            {
                                                null
                                            }
                                        },
                                        zone_id = try
                                        {
                                            ZoneId.of(it[GuildSettingsDataTable.zone_id]
                                                    ?: ZoneId.systemDefault().id)
                                        } catch (e: DateTimeException) {
                                            ZoneId.systemDefault()
                                        },
                                        prefix = it[GuildSettingsDataTable.prefix],

                                        requireReasonForPunish = it[GuildSettingsDataTable.requireReasonForPunish]
                                )
                            }.singleOrNull()
                } ?: EMPTY
                cache.put(guild.idLong, data)
                return data
            }
        }

        fun <T> update(guild: Guild, column: Column<T>, value: T): T?
        {
            cache.pull(guild.idLong)
            return transaction {
                val old = GuildSettingsDataTable
                        .slice(column)
                        .select { GuildSettingsDataTable.guild_id.eq(guild.idLong) }
                        .fetchSize(1)
                        .singleOrNull()

                if(old == null)
                {
                    GuildSettingsDataTable
                            .insert {
                                it[guild_id] = guild.idLong
                                it[column] = value
                            }
                    return@transaction null
                }
                else
                {
                    GuildSettingsDataTable
                            .update({ GuildSettingsDataTable.guild_id.eq(guild.idLong) })
                            {
                                it[column] = value
                            }
                    return@transaction old[column]
                }
            }
        }
    }
}