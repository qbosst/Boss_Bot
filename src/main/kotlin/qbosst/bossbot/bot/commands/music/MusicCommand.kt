package qbosst.bossbot.bot.commands.music

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.database.data.GuildSettingsData

abstract class MusicCommand(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf(),
        private val connect: Boolean = false

) : Command(name, description, usage, examples, aliases, true, userPermissions,
        if(connect) botPermissions.plus(arrayOf(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)) else botPermissions)
{

    final override fun execute(event: MessageReceivedEvent, args: List<String>) {

        if(event.member!!.voiceState?.inVoiceChannel() == true)
        {
            if(event.guild.audioManager.isConnected)
            {
                if(event.member!!.voiceState!!.channel == event.guild.audioManager.connectedChannel)
                {
                    run(event, args)
                }
                else if(connect)
                {
                    if(getMembersConnected(event.guild, false).isEmpty() || event.member!!.isDj())
                    {
                        val channel = event.member!!.voiceState!!.channel!!
                        if(connect(channel))
                        {
                            run(event, args)
                        }
                        else
                        {
                            event.channel.sendMessage("I do not have the following permissions for voice channel `${channel.name}`; `${fullBotPermissions.joinToString("`, `")}`").queue()
                        }
                    }
                    else
                    {
                        onMemberInWrongChannel(event, args)
                    }
                }
                else
                {
                    onMemberInWrongChannel(event, args)
                }
            }
            else if(connect)
            {
                if(getMembersConnected(event.guild, false).isEmpty() || event.member!!.isDj())
                {
                    val channel = event.member!!.voiceState!!.channel!!
                    if(connect(channel))
                    {
                        run(event, args)
                    }
                    else
                    {
                        event.channel.sendMessage("I do not have the following permissions for voice channel `${channel.name}`; `${fullBotPermissions.joinToString("`, `")}`").queue()
                    }
                }
                else
                {
                    onMemberInWrongChannel(event, args)
                }
            }
            else
            {
                onSelfNotConnected(event, args)
            }
        }
        else
        {
            onMemberNotConnected(event, args)
        }
    }

    open fun onSelfNotConnected(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("I am not connected to a voice channel!").queue()
    }

    open fun onMemberNotConnected(event: MessageReceivedEvent, args: List<String>)
    {
        if(event.guild.audioManager.isConnected)
        {
            onSelfNotConnected(event, args)
        }
        else
        {
            event.channel.sendMessage("You must be connected to a voice channel for me to join!").queue()
        }
    }

    open fun onMemberInWrongChannel(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage("You must be connected to my channel to use me!").queue()
    }

    abstract fun run(event: MessageReceivedEvent, args: List<String>)

    protected fun Member.isDj(): Boolean
    {
        val dj = GuildSettingsData.get(guild).getDjRole(guild)
        return if(dj != null)
        {
            roles.contains(dj)
        }
        else
        {
            hasPermission(Permission.ADMINISTRATOR)
        }
    }

    protected fun getMembersConnected(guild: Guild, includeBots: Boolean = false): List<Member>
    {
        val members: List<Member> = guild.audioManager.connectedChannel?.members ?: return emptyList()
        return if(includeBots)
        {
            members
        }
        else
        {
            members.filter { !it.user.isBot }
        }
    }

    protected fun connect(channel: VoiceChannel): Boolean
    {
        val guild = channel.guild
        return if(guild.selfMember.hasPermission(channel, fullBotPermissions))
        {
            guild.audioManager.openAudioConnection(channel)
            true
        }
        else
        {
            false
        }
    }

}