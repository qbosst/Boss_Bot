package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.music.MusicCommand
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueMoveCommand: MusicCommand (
        "move"
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>) {
        //TODO
    }
}