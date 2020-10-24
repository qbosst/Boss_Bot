package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import me.qbosst.bossbot.util.toBooleanOrNull
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueLoopCommand: MusicCommand(
        "loop",
        "Loops the queue",
        usage = listOf("[true|false]"),
        examples = listOf("", "true", "false")
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val manager = GuildMusicManager.get(event.guild).scheduler
        manager.isLooping = args.getOrNull(0)?.toBooleanOrNull() ?: !manager.isLooping
        event.channel.sendMessage("Queue is " + (if(manager.isLooping) "now" else "no longer") + " being looped").queue()
    }
}