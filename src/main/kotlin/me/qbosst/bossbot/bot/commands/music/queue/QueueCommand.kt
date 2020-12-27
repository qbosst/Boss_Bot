package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.util.embed.DescriptionMenuEmbed
import me.qbosst.bossbot.util.loadObjects
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueCommand: MusicCommand(
        "queue",
        requiresMemberConnected = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS),
        children = listOf(QueueClearCommand, QueueCurrentCommand, QueueLoopCommand, QueueReverseCommand,
                QueueShuffleCommand)
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val handler = event.guild.getAudioHandler()
        val queue = handler.getQueue()
        if(queue.isEmpty())
            event.channel.sendMessage("The queue is empty!").queue()
        else
        {
            val page = if(args.isNotEmpty()) args[0].toIntOrNull() ?: 0 else 0

            val embed = DescriptionMenuEmbed(5, queue.mapIndexed { index, track -> "${index+1}. [${track.info.title}](${track.info.uri}) [${event.guild.getMemberById(track.userData.toString().toLongOrNull() ?: 0)?.asMention ?: "N/A"}]\n" })
                    .createPage(EmbedBuilder()
                            .setColor(event.guild.selfMember.colorRaw)
                            .setTitle("Queue for ${event.guild.name}")
                            , page)

            event.channel.sendMessage(embed.build()).queue()
        }
    }
}