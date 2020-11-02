package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.entities.music.GuildMusicManager
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object PlayCommand: MusicCommand(
        "play",
        description = "Plays music through the bot",
        usage = listOf("<track url|track name|playlist>"),
        botPermissions = listOf(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD),
        connect = true
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val query = args.joinToString(" ")
            event.channel.sendMessage("Searching for... `${query.maxLength(64)}`").queue()
            {
                GuildMusicManager.get(event.guild).scheduler.loadAndPlay(it, query)
            }
        }
        else
            event.channel.sendMessage("Please provide the song you would like to play").queue()
    }
}