package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

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