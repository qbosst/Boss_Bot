package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.bot.TICK
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object DisconnectCommand: MusicCommand(
        "disconnect",
        "Disconnects me from your voice channel",
        aliases = listOf("dc")
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        event.guild.audioManager.closeAudioConnection()
        event.message.addReaction(TICK).queue()
    }
}