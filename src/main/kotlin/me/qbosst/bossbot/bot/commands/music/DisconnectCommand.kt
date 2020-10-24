package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.TICK
import me.qbosst.bossbot.entities.music.GuildMusicManager
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object DisconnectCommand: MusicCommand(
        "disconnect",
        description = "Disconnects me from your voice channel",
        aliases = listOf("dc")
), EventListener
{
    private val scheduledLeaves = mutableMapOf<Long, ScheduledFuture<*>>()
    private const val SECONDS_UNTIL_DISCONNECT = 10L

    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        event.guild.audioManager.closeAudioConnection()
        event.message.addReaction(TICK).queue()
    }

    override fun onEvent(event: GenericEvent)
    {
        when(event)
        {
            is GuildVoiceLeaveEvent ->
                onGuildVoiceLeaveEvent(event)
            is GuildVoiceJoinEvent ->
                onGuildVoiceJoinEvent(event)
            is GuildVoiceMoveEvent ->
                onGuildVoiceMoveEvent(event)
            is GuildLeaveEvent ->
                onGuildLeaveEvent(event)
            is StatusChangeEvent ->
                onStatusChangeEvent(event)
        }
    }

    private fun onGuildVoiceLeaveEvent(event: GuildVoiceLeaveEvent)
    {
        if(event.member == event.guild.selfMember)
        {
            val manager = GuildMusicManager.remove(event.guild) ?: return
            manager.scheduler.channel?.sendMessage("I have left `${event.channelLeft.name}`.")?.queue()
        }
        else if(event.channelLeft == event.guild.audioManager.connectedChannel)
            if(getMembersConnected(event.guild, false).isEmpty())
                scheduleTask(event.guild)

    }

    private fun onGuildVoiceMoveEvent(event: GuildVoiceMoveEvent)
    {
        if(event.member == event.guild.selfMember)
            if(getMembersConnected(event.guild, false).isEmpty() && !contains(event.guild))
                scheduleTask(event.guild)

        else if(event.channelJoined == event.guild.audioManager.connectedChannel)
            if(getMembersConnected(event.guild, false).isNotEmpty() && contains(event.guild))
                cancelTask(event.guild)
    }

    private fun onGuildVoiceJoinEvent(event: GuildVoiceJoinEvent)
    {
        if(event.channelJoined == event.guild.audioManager.connectedChannel)
            if(getMembersConnected(event.guild, false).isNotEmpty())
                cancelTask(event.guild)
    }

    private fun onGuildLeaveEvent(event: GuildLeaveEvent)
    {
        GuildMusicManager.remove(event.guild)
    }

    private fun onStatusChangeEvent(event: StatusChangeEvent)
    {
        when(event.newStatus)
        {
            JDA.Status.SHUTTING_DOWN ->
                for(guild in event.jda.guilds)
                {
                    guild.audioManager.closeAudioConnection()
                    GuildMusicManager.remove(guild)
                }
        }
    }

    private fun scheduleTask(guild: Guild)
    {
        val task = BossBot.scheduler.schedule(
                {
                    guild.audioManager.closeAudioConnection()
                },
                SECONDS_UNTIL_DISCONNECT, TimeUnit.SECONDS)
        scheduledLeaves.put(guild.idLong, task)?.cancel(true)
    }

    private fun cancelTask(guild: Guild) = scheduledLeaves.remove(guild.idLong)?.cancel(true)

    private fun contains(guild: Guild): Boolean = scheduledLeaves.contains(guild.idLong)
}