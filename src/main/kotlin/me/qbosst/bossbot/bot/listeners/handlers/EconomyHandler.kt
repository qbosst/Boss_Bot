package me.qbosst.bossbot.bot.listeners.handlers

import me.qbosst.bossbot.database.managers.MemberDataManager
import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime

class EconomyHandler(cacheSize: Int)
{
    private var messageCache = FixedCache<Pair<Long, Long>, MessageMemberStatus>(cacheSize)
    private val voiceCache = mutableMapOf<Long, MutableMap<Long, VoiceMemberStatus>>()

    fun handleMessageReceivedEvent(event: MessageReceivedEvent, isCommandEvent: Boolean)
    {
        val member = (if(event.isFromGuild || event.isWebhookMessage) null else event.member) ?: return
        val key = member.key()
        val messageTimeCreated = event.message.timeCreated

        if(key !in messageCache)
            messageCache.put(key, MessageMemberStatus(messageTimeCreated, 0)) { oldKey, oldValue ->
                // Checks if the record was removed without the cooldown being finished
                if(oldValue.lastMessageDate.plusSeconds(SECONDS_UNTIL_ELIGIBLE).isAfter(OffsetDateTime.now()))
                    messageCache = FixedCache(messageCache.size+25, messageCache)
                            .apply { put(oldKey, oldValue) }
                            .also { LOG.warn("The cache size for ${this::messageCache.name} needs to be increased!") }

                // Update message counter for member removed
                else if(oldValue.messageCount > 0)
                    MemberDataManager.update(oldKey.first, oldKey.second) { old ->
                        old.copy(message_count = old.message_count + oldValue.messageCount)
                    }
            }
        val memberStats = messageCache[key]!!.apply { messageCount += 1 }

        if(isCommandEvent) return

        // update stats
        if(memberStats.lastMessageDate.plusSeconds(SECONDS_UNTIL_ELIGIBLE).isBefore(messageTimeCreated))
            memberStats.apply {
                // update stats in database
                MemberDataManager.update(member) { old ->
                    old.copy(
                            message_count = old.message_count + messageCount,
                            text_chat_time = old.text_chat_time + SECONDS_UNTIL_ELIGIBLE,
                            experience = old.experience + EXPERIENCE_TO_GIVE
                    )
                }

                messageCount = 0
                lastMessageDate = messageTimeCreated
            }
    }

    fun handleGuildVoiceJoinEvent(event: GuildVoiceJoinEvent)
    {
        if(event.member.user.isBot) return
        val guildCache = voiceCache.computeIfAbsent(event.guild.idLong) { mutableMapOf() }
        guildCache[event.member.idLong] = VoiceMemberStatus(OffsetDateTime.now(), event.voiceState.isMuted)
    }

    fun handleGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent)
    {

    }

    fun handleGuildVoiceMuteEvent(event: GuildVoiceMuteEvent)
    {
        voiceCache[event.guild.idLong]?.get(event.member.idLong)?.mute =
                if(event.isMuted) OffsetDateTime.now() else null
    }

    operator fun get(guildId: Long, userId: Long): MemberStatus =
            MemberStatus(
                    messageCache[genKey(guildId, userId)]?.messageCount ?: 0,
                    voiceCache[guildId]?.get(userId)?.voiceChatTime
            )

    operator fun get(member: Member) = get(member.guild.idLong, member.idLong)

    private fun genKey(guildId: Long, userId: Long) = Pair(guildId, userId)

    private fun Member.key() = genKey(guild.idLong, idLong)

    private data class MessageMemberStatus(var lastMessageDate: OffsetDateTime, var messageCount: Int)

    private class VoiceMemberStatus(val join: OffsetDateTime, isMuted: Boolean)
    {
        var mute: OffsetDateTime? = if(isMuted) join else null
            set(value)
            {
                field?.let { mute -> secondsMuted += Duration.between(mute, OffsetDateTime.now()).seconds }
                field = value
            }

        var secondsMuted: Long = 0
            private set

        val voiceChatTime: Duration
            get() = Duration.between(join, OffsetDateTime.now())
    }

    class MemberStatus constructor(val messageCount: Int, val voiceChatTime: Duration?)

    companion object
    {
        private const val SECONDS_UNTIL_ELIGIBLE = 60L
        private const val EXPERIENCE_TO_GIVE = 10 //TODO make adjustable
        private val LOG: Logger = LoggerFactory.getLogger(this::class.java.simpleName)
    }
}