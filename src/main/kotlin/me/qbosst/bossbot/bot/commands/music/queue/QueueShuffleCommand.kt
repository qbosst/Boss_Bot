package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.TICK
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.util.toBooleanOrNull
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueShuffleCommand: MusicCommand(
        "shuffle",
        "Shuffles the queue",
        usage = listOf("[shuffleCurrentTrack]"),
        examples = listOf("true", "false")
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val handler = event.guild.getAudioHandler()


        val shuffleCurrent = args.getOrNull(0)?.toBooleanOrNull() ?: false
        handler.shuffle(shuffleCurrent)
        event.message.addReaction(TICK).queue()
    }
}