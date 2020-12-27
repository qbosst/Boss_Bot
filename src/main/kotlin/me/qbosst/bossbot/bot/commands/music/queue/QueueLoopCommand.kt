package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.util.toBooleanOrNull
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueLoopCommand: MusicCommand(
        "loop",
        "Loops the queue",
        usages = listOf("[true|false]")
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val handler = event.guild.getAudioHandler()
        val value = if(args.isNotEmpty()) args[0].toBooleanOrNull() else !handler.isLooping
        if(value == null)
            event.channel.sendMessage("That is not a valid option!").queue()
        else
        {
            handler.isLooping = value
            event.channel.sendMessage("Looping has been set to `${value}`").queue()
        }
    }
}