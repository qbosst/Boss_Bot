package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.TICK
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.util.toBooleanOrNull
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueReverseCommand: MusicCommand(
        "reverse",
        "Reverses the queue",
        usages = listOf("[reverseCurrentTrack]"),
        examples = listOf("true", "false")
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val handler = event.guild.getAudioHandler()
        
        val reverseCurrent = args.getOrNull(0)?.toBooleanOrNull() ?: false
        handler.reverse(reverseCurrent)
        event.message.addReaction(TICK).queue()
    }
}