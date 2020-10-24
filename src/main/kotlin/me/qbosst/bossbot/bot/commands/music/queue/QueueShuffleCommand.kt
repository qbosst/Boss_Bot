package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.TICK
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueShuffleCommand: MusicCommand(
        "shuffle",
        description = "Shuffles the queue"
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val manager = GuildMusicManager.get(event.guild).scheduler
        manager.shuffle()
        event.message.addReaction(TICK).queue()
    }
}