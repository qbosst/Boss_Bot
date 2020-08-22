package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import me.qbosst.bossbot.util.makeSafe
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueRemoveCommand: MusicCommand(
        "remove"
){

    override fun run(event: MessageReceivedEvent, args: List<String>) {
        val index = if(args.isNotEmpty())
        {
            if(args[0].toIntOrNull() != null)
            {
                args[0].toInt()
            }
            else
            {
                event.channel.sendMessage("`${args[0].makeSafe()}` is not a valid page number").queue()
                return
            }
        }
        else
        {
            event.channel.sendMessage("Please provide the index of which song you would like to delete").queue()
            return
        }

        val removed = GuildMusicManager.get(event.guild).removeTrack(index-1)
        if(removed == null)
        {
            event.channel.sendMessage("I could not find any track at index $index").queue()
        }
        else
        {
            event.channel.sendMessage("Successfully removed ${removed.info.title}").queue()
        }
    }
}