package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.TICK
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueReverseCommand : MusicCommand(
        "reverse",
        botPermissions = listOf(Permission.MESSAGE_ADD_REACTION)
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>) {
        GuildMusicManager.get(event.guild).reverse()
        event.message.addReaction(TICK).queue()
    }

}