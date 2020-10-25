package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.argumentMissing
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueRemoveCommand: MusicCommand(
        "remove",
        description = "Removes a track from the queue",
        usage = listOf("<track index>")
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val index = (args.getOrNull(0) ?: kotlin.run {
            event.channel.sendMessage(argumentMissing("index of the track you would like to remove")).queue()
            return
        }).toIntOrNull() ?: kotlin.run {
            event.channel.sendMessage(argumentInvalid(args[0], "index number")).queue()
            return
        }

        val manager = GuildMusicManager.get(event.guild).scheduler
        val track = manager.removeTrack(index-1)
        if(track == null)
            event.channel.sendMessage("I could not find any track indexed at `$index`").queue()
        else
            event.channel.sendMessage("Successfully removed: `${track.info.title}`").queue()
    }
}