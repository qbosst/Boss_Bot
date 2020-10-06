package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.music.CurrentTrackCommand
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import me.qbosst.bossbot.util.embed.DescriptionMenuEmbed
import me.qbosst.bossbot.util.makeSafe
import me.qbosst.bossbot.util.secondsToString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Duration
import java.time.Instant

object QueueCommand : MusicCommand(
        "queue",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
)
{
    init
    {
        addCommands(setOf(QueueRemoveCommand, QueueReverseCommand, QueueLoopCommand, QueueShuffleCommand, QueueMoveCommand))
    }

    override fun onSelfNotConnected(event: MessageReceivedEvent, args: List<String>) {
        event.channel.sendMessage("I am not connected to a voice channel").queue()
    }

    override fun onMemberNotConnected(event: MessageReceivedEvent, args: List<String>) {
        run(event, args)
    }

    override fun onMemberInWrongChannel(event: MessageReceivedEvent, args: List<String>) {
        run(event, args)
    }

    override fun run(event: MessageReceivedEvent, args: List<String>) {
        val manager = GuildMusicManager.get(event.guild)

        val queue = manager.getQueue()
        if(manager.getQueue().isEmpty())
        {
            event.channel.sendMessage("The queue is empty!").queue()
        }
        else
        {
            val formattedQueue = queue
                    .mapIndexed { index, track ->
                        val sb = StringBuilder("${index+1}. [${track.info.title}](${track.info.uri})")

                        if(track == manager.currentTrack)
                        {
                            sb.insert(0, "**").append("**")
                        }
                        sb.append("\n").toString()
                    }

            val menu = DescriptionMenuEmbed(5, formattedQueue)

            val page = if(args.isNotEmpty())
            {
                args[0].toIntOrNull() ?: run {
                    event.channel.sendMessage("`${args[0].makeSafe()}` is not a valid page number!").queue()
                    return
                }
            } else 0

            val queueLength: Long = kotlin.run {
                val length: Long = manager.getQueue(false).map { it.duration }.sum()
                val current: Instant? = CurrentTrackCommand.getCurrent(event.guild)
                if(current != null && manager.currentTrack != null)
                {
                    length + (manager.currentTrack!!.duration - Duration.between(current, Instant.now()).toMillis())
                }
                else
                {
                    length
                }
            }

            event.channel.sendMessage(menu.createPage(EmbedBuilder()
                    .setTitle("Queue for ${event.guild.name}")
                    .setFooter("Queue Length : ${secondsToString(queueLength / 1000)}"),
                    page).build()).queue()
        }
    }

}
