package qbosst.bossbot.bot.commands.music.queue

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.music.MusicCommand
import qbosst.bossbot.entities.music.GuildMusicManager

object QueueLoopCommand: MusicCommand(
        "loop"
) {
    override fun run(event: MessageReceivedEvent, args: List<String>) {
        val isLooping = GuildMusicManager.get(event.guild).loop()
        if(isLooping)
        {
            event.channel.sendMessage("Queue is now looping!").queue()
        }
        else
        {
            event.channel.sendMessage("Queue is no longer being looped").queue()
        }
    }

}