package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.listeners.MessageListener
import me.qbosst.bossbot.entities.database.GuildSettingsData
import me.qbosst.bossbot.util.dateTimeFormatter
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

object DebugCommand : DeveloperCommand(
        "debug",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            if(MessageListener.getCommand(args[0]) != null)
            {
                var command: Command = MessageListener.getCommand(args[0])!!
                var index = 1
                while (index < args.size)
                {
                    val newCommand = command.getCommand(args[index])

                    if(newCommand != null)
                    {
                        command = newCommand
                        index++
                    }
                    else break
                }

                try
                {
                    //command.run(event, args.drop(index))
                }
                catch (e: Exception)
                {
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    event.channel.sendMessage("Caught Exception: ```$sw```".maxLength(Message.MAX_CONTENT_LENGTH)).queue()
                }
            }
            else
                event.channel.sendMessage("Could not find command `${args[0]}`").queue()
        }
        else
        {
            val totalMb = Runtime.getRuntime().totalMemory() / (1024*1024)
            val usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024)

            val zoneId = GuildSettingsData.get(if(event.isFromGuild) event.guild else null).zone_id
            val date = dateTimeFormatter.format(BossBot.startUp.atZoneSameInstant(zoneId))

            val embed = EmbedBuilder()
                    .setTitle("${event.jda.selfUser.asTag} statistics")
                    .addField("Startup Time", "$date ${TimeZone.getTimeZone(zoneId).getDisplayName(true, TimeZone.SHORT)}", true)
                    .addField("Memory Usage", "${usedMb}MB / ${totalMb}MB", true)
                    .addField("Guilds", BossBot.shards.guilds.size.toString(), true)

            event.channel.sendMessage(embed.build()).queue()
        }
    }

}