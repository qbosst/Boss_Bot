package qbosst.bossbot.bot.commands.dev

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.BossBot
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.bot.listeners.Listener
import qbosst.bossbot.database.data.GuildSettingsData
import qbosst.bossbot.util.dateTimeFormatter
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
            if(Listener.getCommand(args[0]) != null)
            {
                var command: Command = Listener.getCommand(args[0])!!
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
                    var exception = "Caught Exception: ```$sw```"
                    exception = if(exception.length > Message.MAX_CONTENT_LENGTH)
                    {
                        val msg = "...\nCheck console for full details```"
                        exception.substring(0, Message.MAX_CONTENT_LENGTH - msg.length) + msg
                    }
                    else exception

                    event.channel.sendMessage(exception).queue()
                }
            }
            else
            {
                event.channel.sendMessage("Could not find command `${args[0]}`").queue()
            }
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