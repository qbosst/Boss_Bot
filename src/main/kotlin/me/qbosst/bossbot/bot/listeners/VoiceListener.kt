package me.qbosst.bossbot.bot.listeners

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.PASTEL_GREEN
import me.qbosst.bossbot.bot.PASTEL_RED
import me.qbosst.bossbot.bot.PASTEL_YELLOW
import me.qbosst.bossbot.database.managers.MemberDataManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.entities.music.GuildAudioHandler
import me.qbosst.bossbot.util.TimeUtil
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime

object VoiceListener: EventListener
{
    private val voiceCache = mutableMapOf<Long, MutableMap<Long, VoiceMemberStatus>>()
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private const val seconds_until_eligible = 60L
    private const val xp_to_give = 10 //TODO make adjustable

    override fun onEvent(event: GenericEvent)
    {
        when(event)
        {
            is GuildVoiceJoinEvent ->
                onGuildVoiceJoinEvent(event)
            is GuildVoiceLeaveEvent ->
                onGuildVoiceLeaveEvent(event)
            is GuildVoiceMuteEvent ->
                onGuildVoiceMuteEvent(event)
            is GuildVoiceMoveEvent ->
                onGuildVoiceMoveEvent(event)

            is StatusChangeEvent ->
                onStatusChangeEvent(event)

        }
    }

    private fun onGuildVoiceJoinEvent(event: GuildVoiceJoinEvent)
    {
        // Logs the guild voice join event for stats
        if(!event.member.user.isBot)
        {
            val guildId = event.guild.idLong
            if(!voiceCache.containsKey(guildId))
                voiceCache[guildId] = mutableMapOf()
            voiceCache[guildId]!![event.member.idLong] = VoiceMemberStatus(OffsetDateTime.now(), event.voiceState.isMuted)
        }

        // Logs the guild voice join event for logging
        event.guild.getSettings().getVoiceLogsChannel(event.guild)
                ?.sendMessage(EmbedBuilder()
                        .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                        .setDescription("**${event.member.asMention}** has joined `${event.channelJoined.name}`")
                        .setFooter("User ID : ${event.member.idLong}")
                        .setTimestamp(OffsetDateTime.now())
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .setColor(PASTEL_GREEN)
                        .build())
                ?.queue()
    }

    private fun onGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent)
    {
        val stats = voiceCache[event.guild.idLong]?.remove(event.member.idLong)
        if(stats != null)
            saveVoiceData(event.guild, event.member.idLong, stats)

        // Logs the guild voice leave event for logging
        event.guild.getSettings().getVoiceLogsChannel(event.guild)
                ?.sendMessage(EmbedBuilder()
                        .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                        .setDescription("**${event.member.user.asMention}** has left `${event.channelLeft.name}`")
                        .setFooter("User ID : ${event.member.idLong}")
                        .setTimestamp(OffsetDateTime.now())
                        .setColor(PASTEL_RED)
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .build())
                ?.queue()

        if(event.member == event.guild.selfMember)
            GuildAudioHandler.destroy(event.guild)
    }

    private fun onGuildVoiceMoveEvent(event: GuildVoiceMoveEvent)
    {
        // Logs the guild voice move event for logging
        event.guild.getSettings().getVoiceLogsChannel(event.guild)
                ?.sendMessage(EmbedBuilder()
                        .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                        .setDescription("**${event.member.user.asMention}** has switched channels: `${event.channelLeft.name}` -> `${event.channelJoined.name}`")
                        .setFooter("User ID : ${event.member.idLong}")
                        .setTimestamp(OffsetDateTime.now())
                        .setColor(PASTEL_YELLOW)
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .build())
                ?.queue()
    }

    private fun onGuildVoiceMuteEvent(event: GuildVoiceMuteEvent)
    {
        // Logs the guild voice mute event for stats
        voiceCache[event.guild.idLong]?.get(event.member.idLong)?.setLastMute(if(event.isMuted) OffsetDateTime.now() else null)
    }

    private fun onStatusChangeEvent(event: StatusChangeEvent)
    {
        when(event.newStatus)
        {
            // Sets the status for the jda when the it is finished loading
            JDA.Status.CONNECTED ->
            {
                // Adds all the members in vc in a guild to the voice cache to log stats
                for(guild in event.jda.guilds)
                    voiceCache[guild.idLong] = loadVoiceData(guild)
            }
            JDA.Status.DISCONNECTED ->
                for(guild in event.jda.guilds)
                    saveVoiceData(guild, voiceCache.remove(guild.idLong) ?: continue)

            JDA.Status.SHUTTING_DOWN ->
                for(guild in event.jda.guilds)
                    saveVoiceData(guild, voiceCache.remove(guild.idLong) ?: continue)
        }
    }

    private fun saveVoiceData(guild: Guild, data: MutableMap<Long, VoiceMemberStatus>)
    {
        for(record in data)
            saveVoiceData(guild, record.key, record.value)
    }

    private fun saveVoiceData(guild: Guild, userId: Long, data: VoiceMemberStatus)
    {
        val member = guild.getMemberById(userId)
        if(member?.voiceState != null && member.voiceState!!.isMuted)
            data.setLastMute(OffsetDateTime.now())

        val total = Duration.between(data.join, OffsetDateTime.now()).seconds
        val unMuted = total - data.secondsMuted
        log.debug("${guild.name} (${guild.id}): ${member?.user?.asTag ?: ""} (${userId}) has; spent ${TimeUtil.timeToString(total)} in vc, spent ${TimeUtil.timeToString(unMuted)} un-muted and ${TimeUtil.timeToString(data.secondsMuted)}s muted in vc.")

        val loop: Long = (total - data.secondsMuted) / seconds_until_eligible
        MemberDataManager.update(guild.idLong, userId) { old ->
            return@update old.copy(
                    experience = old.experience + (loop * xp_to_give).toInt(),
                    voice_chat_time = old.voice_chat_time + total
            )
        }
    }

    private fun loadVoiceData(guild: Guild): MutableMap<Long, VoiceMemberStatus>
    {
        val data = mutableMapOf<Long, VoiceMemberStatus>()

        val now = OffsetDateTime.now()
        for(channel in guild.voiceChannels)
            for(member in channel.members)
                if(!member.user.isBot)
                    data[member.idLong] = VoiceMemberStatus(now, member.voiceState!!.isMuted)

        return data
    }

    fun getCachedVoiceChatTime(guild: Guild, userId: Long): Long
    {
        val time = voiceCache[guild.idLong]?.get(userId)?.join ?: return 0
        return Duration.between(time, OffsetDateTime.now()).seconds
    }

    fun getCachedVoiceChatTime(member: Member): Long = getCachedVoiceChatTime(member.guild, member.idLong)

    /**
     *  Class used to track voice member stats like time spent in vc, time spent muted in vc
     */
    private class VoiceMemberStatus(
            val join: OffsetDateTime,
            isMuted: Boolean
    )
    {
        // Time of the mute
        var mute: OffsetDateTime? = if(isMuted) join else null
            private set

        // Seconds that the member has already been muted for
        var secondsMuted: Long = 0
            private set

        /**
         *  Used when member mutes or un-mutes their mic so that it can be logged.
         */
        fun setLastMute(mute: OffsetDateTime?)
        {
            if(this.mute != null)
                secondsMuted += Duration.between(this.mute, OffsetDateTime.now()).seconds

            this.mute = mute
        }
    }
}