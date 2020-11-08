package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.util.toBooleanOrNull
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object PauseCommand: MusicCommand(
        "pause",
        "Pauses the currently playing song",
        usage = listOf("[true|false]")
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val handler = event.guild.getAudioHandler()
        val value = if(args.isNotEmpty()) args[0].toBooleanOrNull() else !handler.paused
        if(value == null)
            event.channel.sendMessage("That is not a valid option!").queue()
        else
        {
            handler.paused = value
            event.channel.sendMessage("Paused has been set to `${value}`").queue()
        }
    }
}