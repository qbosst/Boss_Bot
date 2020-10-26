package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.entities.music.GuildMusicManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 *  @param connect Whether or not to automatically connect if the bot is not already connected. Default is false
 */
abstract class MusicCommand(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf(),
        private val connect: Boolean = false,
        private val requiresMemberConnected: Boolean = true,
        private val requiresSelfConnected: Boolean = true

): Command(name, description, usage, examples, aliases, true, userPermissions, botPermissions)
{
    final override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val member = event.member!!
        if(event.guild.audioManager.isConnected)
        {
            if(member.voiceState!!.inVoiceChannel())
            {
                if(event.guild.audioManager.connectedChannel == member.voiceState!!.channel)
                    run(event, args)
                else if(!requiresMemberConnected)
                    run(event, args)
                else if(connect)
                    attemptConnection(event, args)
                else
                    onMemberInWrongChannel(event, args)
            }
            else if(!requiresMemberConnected)
                run(event, args)
            else
                onMemberNotConnected(event, args)
        }
        else if(!requiresSelfConnected)
            run(event, args)
        else if(connect)
            attemptConnection(event, args)
        else
            onSelfNotConnected(event, args)
    }

    open fun onSelfNotConnected(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("I am not connected to a voice channel!").queue()
    }

    open fun onMemberNotConnected(event: MessageReceivedEvent, args: List<String>)
    {
        if(!event.guild.audioManager.isConnected)
            onSelfNotConnected(event, args)
        else
            event.channel.sendMessage("You must be connected to a voice channel for me to join!").queue()
    }

    open fun onMemberInWrongChannel(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("You must be connected to my channel to use me!").queue()
    }

    abstract fun run(event: MessageReceivedEvent, args: List<String>)

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

    private fun attemptConnection(event: MessageReceivedEvent, args: List<String>)
    {
        val channel = event.member!!.voiceState!!.channel!!
        if(getMembersConnected(channel.guild, false).isEmpty())
            if(connect(channel))
            {
                val manager = GuildMusicManager.get(event.guild)
                manager.scheduler.channelId = event.channel.idLong
                run(event, args)
            }
            else
                event.channel.sendMessage("I cannot connect to this channel").queue()
        else
            onMemberInWrongChannel(event, args)
    }
}