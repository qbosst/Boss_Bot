package qbosst.bossbot.bot.commands.music

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.TICK
import qbosst.bossbot.entities.music.GuildMusicManager

object PauseCommand : MusicCommand(
        "pause",
        botPermissions = listOf(Permission.MESSAGE_ADD_REACTION)
){
    override fun run(event: MessageReceivedEvent, args: List<String>) {
        val manager = GuildMusicManager.get(event.guild)
        if(manager.getQueue().isEmpty())
        {
            event.channel.sendMessage("There is nothing in the queue!").queue()
        }
        else
        {
            manager.paused = !manager.paused
            event.message.addReaction(TICK).queue()
        }
    }
}