package qbosst.bossbot.bot.commands.music.queue

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.TICK
import qbosst.bossbot.bot.commands.music.MusicCommand
import qbosst.bossbot.entities.music.GuildMusicManager

object QueueShuffleCommand : MusicCommand(
        "shuffle",
        botPermissions = listOf(Permission.MESSAGE_ADD_REACTION)
) {
    override fun run(event: MessageReceivedEvent, args: List<String>) {
        GuildMusicManager.get(event.guild).shuffle()
        event.message.addReaction(TICK).queue()
    }
}