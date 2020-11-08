package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.entities.music.GuildAudioHandler
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class MusicCommand(name: String,
                            description: String = "none",
                            usage: List<String> = listOf(),
                            examples: List<String> = listOf(),
                            aliases: List<String> = listOf(),
                            userPermissions: List<Permission> = listOf(),
                            botPermissions: List<Permission> = listOf(),
                            private val autoConnect: Boolean = false,
                            private val requiresSelfConnected: Boolean = true,
                            private val requiresMemberConnected: Boolean = true


): Command(name, description, usage, examples, aliases, true, userPermissions, if(autoConnect) botPermissions.plus(Permission.VOICE_CONNECT) else botPermissions)
{

    final override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val member = event.member!!
        if(event.guild.audioManager.isConnected)
        {
            if(member.voiceState?.inVoiceChannel() == true)
            {
                if(event.guild.audioManager.connectedChannel == member.voiceState?.channel)
                {
                    run(event, args)
                }
                else if(!requiresMemberConnected)
                {
                    run(event, args)
                }
                else if(autoConnect)
                {
                    connect(event, args)
                }
                else
                {
                    onMemberInWrongChannel(event, args)
                }
            }
            else if(!requiresSelfConnected)
            {
                run(event, args)
            }
            else
            {
                onMemberNotConnected(event, args)
            }
        }
        else if(!requiresSelfConnected)
        {
            run(event, args)
        }
        else if(autoConnect)
        {
            connect(event, args)
        }
        else
        {
            onSelfNotConnected(event, args)
        }
    }

    abstract fun run(event: MessageReceivedEvent, args: List<String>)

    open fun onMemberInWrongChannel(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("You must be connected to my channel to use me!").queue()
    }

    open fun onMemberNotConnected(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("You must be connected to a voice channel for me to join!").queue()
    }

    open fun onSelfNotConnected(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("I am not connected to a voice channel!").queue()
    }

    private fun connect(event: MessageReceivedEvent, args: List<String>)
    {
        val channel = event.member?.voiceState?.channel
        if(channel == null)
            onMemberNotConnected(event, args)
        else if(getMembersConnected(channel.guild, false).isEmpty())
            if(connect(channel))
                run(event, args)
            else
                event.channel.sendMessage("I cannot connect to this channel").queue()
        else
            onMemberInWrongChannel(event, args)
    }

    protected fun getMembersConnected(guild: Guild, includeBots: Boolean = false): List<Member>
    {
        val members = guild.audioManager.connectedChannel?.members ?: return listOf()
        return if(includeBots) members else members.filter { !it.user.isBot }
    }

    protected fun connect(channel: VoiceChannel): Boolean
    {
        val guild = channel.guild
        return if(guild.selfMember.hasPermission(channel, fullBotPermissions))
        {
            guild.audioManager.openAudioConnection(channel)
            true
        } else false
    }

    final fun Guild.getAudioHandler(): GuildAudioHandler = GuildAudioHandler.get(this)
}