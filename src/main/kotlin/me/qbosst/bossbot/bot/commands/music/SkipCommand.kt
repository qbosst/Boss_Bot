package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.bot.Constants
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object SkipCommand: MusicCommand(
        "skip",
        "Skips the current track"
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val handler = event.guild.getAudioHandler()
        if(handler.getQueue().isEmpty())
            event.channel.sendMessage("There is nothing in the queue!").queue()
        else
        {
            handler.nextTrack()
            event.message.addReaction(Constants.TICK).queue()
        }
    }
}