package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import me.qbosst.bossbot.util.embed.DescriptionMenuEmbed
import me.qbosst.bossbot.util.loadObjects
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueCommand: MusicCommand(
        "queue",
        requiresMemberConnected = false
)
{
    init
    {
        addCommands(loadObjects(this::class.java.`package`.name, Command::class.java).filter { it != this })
    }

    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val manager = GuildMusicManager.get(event.guild).scheduler

        val queue = manager.getQueue()
        if(queue.isEmpty())
            event.channel.sendMessage("The queue is empty!").queue()
        else
        {
            val formattedQueue = queue.mapIndexed { index, track ->
                val sb = StringBuilder("${index+1}. [${track.info.title}](${track.info.uri})")
                if(track == manager.currentTrack)
                    sb.insert(0, "**").append("**")
                sb.append("\n").toString()
            }

            val menu = DescriptionMenuEmbed(5, formattedQueue)
            val page = args.getOrNull(0)?.toIntOrNull() ?: 0

            event.channel.sendMessage(menu.createPage(EmbedBuilder()
                    .setTitle("Queue for ${event.guild.name}")
                    .setColor(event.guild.selfMember.colorRaw)
                    , page).build()).queue()
        }
    }
}