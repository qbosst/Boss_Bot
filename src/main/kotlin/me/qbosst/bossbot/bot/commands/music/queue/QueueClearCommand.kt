package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.Constants
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.util.toBooleanOrNull
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueClearCommand: MusicCommand(
        "clear",
        "Clears the whole queue",
        usages = listOf("[clearCurrentTrack]"),
        examples = listOf("true", "false")
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val handler = event.guild.getAudioHandler()

        val clearCurrent = args.getOrNull(0)?.toBooleanOrNull() ?: true
        handler.clear(clearCurrent)

        event.message.addReaction(Constants.TICK).queue()
    }
}