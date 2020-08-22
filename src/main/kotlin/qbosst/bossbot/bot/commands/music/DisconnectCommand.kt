package qbosst.bossbot.bot.commands.music

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import qbosst.bossbot.bot.BossBot
import qbosst.bossbot.bot.TICK
import qbosst.bossbot.entities.music.GuildMusicManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object DisconnectCommand: MusicCommand(
        "disconnect",
        aliases = listOf("dc"),
        botPermissions = listOf(Permission.MESSAGE_ADD_REACTION)
), EventListener
{

    private val scheduledLeave = mutableMapOf<Long, ScheduledFuture<*>>()

    override fun onMemberNotConnected(event: MessageReceivedEvent, args: List<String>) {
        val isDj = event.member!!.isDj()
        if(isDj)
        {
            run(event, args)
        }
        else
        {
            event.channel.sendMessage("You must be in my channel to disconnect me!").queue()
        }
    }

    override fun run(event: MessageReceivedEvent, args: List<String>) {
        event.guild.audioManager.closeAudioConnection()
        event.message.addReaction(TICK).queue()
    }

    override fun onEvent(event: GenericEvent) {
        when(event)
        {
            is GuildVoiceLeaveEvent ->
            {
                if(event.member == event.guild.selfMember)
                {
                    GuildMusicManager.remove(event.guild)
                }
                else if(event.channelLeft == event.guild.audioManager.connectedChannel)
                {
                    if(getMembersConnected(event.guild, false).isEmpty())
                    {
                        println(true)
                        val task = BossBot.threadpool.schedule({
                            event.guild.audioManager.closeAudioConnection()
                        }, 2, TimeUnit.MINUTES)
                        scheduledLeave.put(event.guild.idLong, task)?.cancel(true)
                    }
                }
            }
            is GuildLeaveEvent ->
            {
                GuildMusicManager.remove(event.guild)
            }

            is StatusChangeEvent ->
            {
                if(event.newStatus == JDA.Status.SHUTTING_DOWN)
                {
                    for(guild in event.jda.guilds)
                    {
                        guild.audioManager.closeAudioConnection()
                    }
                }
            }

        }
    }
}