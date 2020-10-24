package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.bot.TICK
import me.qbosst.bossbot.entities.music.GuildMusicManager
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object SkipCommand: MusicCommand(
        "skip",
        description = "Skips the current track"
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val manager = GuildMusicManager.get(event.guild).scheduler
        if(manager.getQueue().isEmpty())
            event.channel.sendMessage("There is nothing in the queue!").queue()
        else
        {
            manager.nextTrack()
            event.message.addReaction(TICK).queue()
        }
    }
}