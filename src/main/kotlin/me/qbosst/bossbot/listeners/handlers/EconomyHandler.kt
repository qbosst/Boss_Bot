package me.qbosst.bossbot.listeners.handlers

import me.qbosst.bossbot.database.manager.MemberData
import me.qbosst.bossbot.database.manager.MemberDataManager
import me.qbosst.jda.ext.util.FixedCache
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime

private const val SECONDS_UNTIL_ELIGIBLE = 60L
private const val EXPERIENCE_TO_GIVE = 10
private val LOG: Logger = LoggerFactory.getLogger(EconomyHandler::class.java)

class EconomyHandler(cacheSize: Int) {
    private var messageCache = FixedCache<Pair<Long, Long>, MessageMemberInfo>(cacheSize)
    private val voiceCache = mutableMapOf<Long, MutableMap<Long, VoiceMemberInfo>>()

    fun handleMessageReceivedEvent(event: MessageReceivedEvent, isCommandEvent: Boolean) {
        // do not give xp to bots and make sure event is from guild
        if(!event.isFromGuild || event.isWebhookMessage || event.author.isBot)
            return
        val member = event.member!!
        val key = member.key()
        val messageTimeCreated = event.message.timeCreated

        if(key !in messageCache) {
            messageCache.put(key, MessageMemberInfo(messageTimeCreated, 0)) { oldKey, oldValue ->

                // check if the record's last message date was within 60 seconds
                if(oldValue.lastMessageDate.plusSeconds(SECONDS_UNTIL_ELIGIBLE).isAfter(OffsetDateTime.now())) {
                    messageCache = FixedCache(messageCache.size+25, messageCache)
                        .apply { put(oldKey, oldValue) }
                        .also { LOG.warn("The cache size for ${this::messageCache.name} needs to be increased!") }
                }

                // update message counter for member removed
                else if(oldValue.messageCount > 0) {
                    val (guildId, userId) = oldKey
                    MemberDataManager.update(guildId, userId) { old ->
                        return@update old.copy(messageCount = old.messageCount + oldValue.messageCount)
                    }
                }
            }
        }

        val memberStats = messageCache[key]!!.apply { messageCount += 1 }

        if(isCommandEvent)
            return

        // check if last message date was after 60 seconds
        if(memberStats.lastMessageDate.plusSeconds(SECONDS_UNTIL_ELIGIBLE).isBefore(OffsetDateTime.now())) {
            memberStats.apply {
                MemberDataManager.update(member) { old ->
                    return@update old.copy(
                        experience = old.experience + EXPERIENCE_TO_GIVE,
                        textChatTime = old.textChatTime + SECONDS_UNTIL_ELIGIBLE,
                        messageCount = old.messageCount + messageCount
                    )
                }

                messageCount = 0
                lastMessageDate = messageTimeCreated
            }
        }
    }

    fun handleGuildVoiceJoinEvent(event: GuildVoiceJoinEvent) {
        if(event.member.user.isBot) {
            return
        }

        val guildCache = voiceCache.computeIfAbsent(event.guild.idLong) { mutableMapOf() }
        guildCache[event.member.idLong] = VoiceMemberInfo(OffsetDateTime.now(), event.voiceState.isMuted)
    }

    fun handleGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent) {
        val state = voiceCache[event.guild.idLong]?.remove(event.member.idLong) ?: return
        MemberDataManager.update(event.member) { old ->
            return@update process(state, old, event.voiceState.isMuted)
        }
    }

    fun handleGuildVoiceMuteEvent(event: GuildVoiceMuteEvent) {
        val memberCache = voiceCache[event.guild.idLong]?.get(event.member.idLong)
            ?: return
        memberCache.mute = if(event.isMuted) OffsetDateTime.now() else null
    }

    fun save(jda: JDA) {
        // update voice stats
        jda.guildCache.forEach { guild ->
            val guildId = guild.idLong
            if(voiceCache.containsKey(guild.idLong)) {
                voiceCache.remove(guild.idLong)!!.forEach { (userId, state) ->
                    MemberDataManager.update(guildId, userId) { old ->
                        val isMuted = guild.getMemberById(userId)?.voiceState?.isMuted ?: false
                        return@update process(state, old, isMuted)
                    }
                }
            }
        }
    }

    private fun process(state: VoiceMemberInfo, data: MemberData, isMutedNow: Boolean): MemberData {
        val now = OffsetDateTime.now()
        if(isMutedNow) {
            state.mute = now
        }

        val total = Duration.between(state.join, now).seconds
        val xp: Int = (((total - state.secondsMuted) / SECONDS_UNTIL_ELIGIBLE) * EXPERIENCE_TO_GIVE).toInt()

        return data.copy(
            experience = data.experience + xp,
            voiceChatTime = data.voiceChatTime + total
        )

    }

    operator fun get(guildId: Long, userId: Long) = Pair(
        messageCache[genKey(guildId, userId)]?.messageCount ?: 0,
        voiceCache[guildId]?.get(userId)?.voiceChatTime
    )

    operator fun get(member: Member) = get(member.guild.idLong, member.idLong)
}

private data class MessageMemberInfo(var lastMessageDate: OffsetDateTime, var messageCount: Int)

/**
 * Keeps track of a [Member]'s voice state
 *
 * @param join the time the [Member] has joined a Voice Channel
 * @param isMuted whether the [Member] joined muted or not
 */
private class VoiceMemberInfo(val join: OffsetDateTime, isMuted: Boolean) {
    var mute: OffsetDateTime? = if(isMuted) join else null
        set(value) {
            field?.let { mute -> secondsMuted += Duration.between(mute, OffsetDateTime.now()).seconds }
            field = value
        }

    var secondsMuted: Long = 0
        private set

    val voiceChatTime: Duration
        get() = Duration.between(join, OffsetDateTime.now())
}

private fun genKey(guildId: Long, userId: Long) = Pair(guildId, userId)

private fun Member.key() = genKey(guild.idLong, idLong)



