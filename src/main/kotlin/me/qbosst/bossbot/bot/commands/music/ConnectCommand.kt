package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.bot.TICK
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ConnectCommand: MusicCommand(
        "connect",
        "Connects me to your channel",
        connect = true
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        event.message.addReaction(TICK).queue()
    }
}