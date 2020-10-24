package me.qbosst.bossbot.bot.listeners

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.database.managers.MemberDataManager
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.database.tables.MemberDataTable
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
import java.awt.Color
import java.time.Duration
import java.time.OffsetDateTime

object VoiceListener: EventListener
{
    private val voiceCache = mutableMapOf<Pair<Long, Long>, VoiceMemberStatus>()

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
            voiceCache[Pair(event.guild.idLong, event.member.idLong)] = VoiceMemberStatus(OffsetDateTime.now(), event.voiceState.isMuted)

        // Logs the guild voice join event for logging
        event.guild.getSettings().getVoiceLogsChannel(event.guild)
                ?.sendMessage(EmbedBuilder()
                        .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                        .setDescription("**${event.member.asMention}** has joined `${event.channelJoined.name}`")
                        .setFooter("User ID : ${event.member.idLong}")
                        .setTimestamp(OffsetDateTime.now())
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .setColor(Color.GREEN)
                        .build())
                ?.queue()
    }

    private fun onGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent)
    {
        // Logs the guild voice leave event for stats
        if(!event.member.user.isBot)
        {
            val key = Pair(event.guild.idLong, event.member.idLong)

            val stats = voiceCache.remove(key) ?: return
            val now = OffsetDateTime.now()
            if(event.voiceState.isMuted)
                stats.update(now)

            val total = Duration.between(stats.join, now).seconds
            val loop: Long = (total - stats.secondsMuted) / seconds_until_eligible

            // Updates stats
            MemberDataManager.update(event.member,
                    { insert ->
                        insert[MemberDataTable.experience] = xp_to_give
                        insert[MemberDataTable.voice_chat_time] = total
                    },
                    { rs, update ->
                        update[MemberDataTable.experience] = rs[MemberDataTable.experience] + (xp_to_give * loop).toInt()
                        update[MemberDataTable.voice_chat_time] = rs[MemberDataTable.voice_chat_time] + total
                    })

            BossBot.LOG.debug("[${event.guild.name}] ${event.member.user.asTag} has spent a total of ${TimeUtil.secondsToString(total)} in vc, ${TimeUtil.secondsToString(stats.secondsMuted)} muted")
        }

        // Logs the guild voice leave event for logging
        event.guild.getSettings().getVoiceLogsChannel(event.guild)
                ?.sendMessage(EmbedBuilder()
                        .setAuthor(event.member.effectiveName, null, event.member.user.avatarUrl)
                        .setDescription("**${event.member.user.asMention}** has left `${event.channelLeft.name}`")
                        .setFooter("User ID : ${event.member.idLong}")
                        .setTimestamp(OffsetDateTime.now())
                        .setColor(Color.RED)
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .build())
                ?.queue()
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
                        .setColor(Color.YELLOW)
                        .setThumbnail(event.member.user.effectiveAvatarUrl)
                        .build())
                ?.queue()
    }

    private fun onGuildVoiceMuteEvent(event: GuildVoiceMuteEvent)
    {
        // Logs the guild voice mute event for stats
        if(!event.member.user.isBot)
            voiceCache[Pair(event.guild.idLong, event.member.idLong)]?.update(if(event.isMuted) OffsetDateTime.now() else null)
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
                    for(vc in guild.voiceChannels)
                        for(member in vc.members)
                            if(member.voiceState != null)
                                voiceCache[Pair(guild.idLong, member.idLong)] = VoiceMemberStatus(OffsetDateTime.now(), member.voiceState!!.isMuted)
            }
        }
    }

    fun getCachedVoiceChatTime(guild: Guild, userId: Long): Long
    {
        return Duration.between((voiceCache[Pair(guild.idLong, userId)]?.join ?: return 0), OffsetDateTime.now()).seconds
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
        fun update(mute: OffsetDateTime?)
        {
            if(this.mute != null)
                secondsMuted += Duration.between(this.mute, OffsetDateTime.now()).seconds

            this.mute = mute
        }
    }
}