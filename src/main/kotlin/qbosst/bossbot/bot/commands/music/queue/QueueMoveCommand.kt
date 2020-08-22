package qbosst.bossbot.bot.commands.music.queue

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.music.MusicCommand

object QueueMoveCommand: MusicCommand (
        "move"
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>) {
        //TODO
    }
}