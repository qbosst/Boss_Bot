package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.argumentInvalid
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object OnlineStatusCommand : DeveloperCommand(
        "onlinestatus",
        description = "Sets the self-user's online status",
        usage = listOf("<online status>"),
        examples = enumValues<OnlineStatus>().map { it.name.toLowerCase() },
        guildOnly = false
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
        {
            val status = enumValues<OnlineStatus>().firstOrNull { it.name.equals(args[0], true) }
            if(status == null)
                event.channel.sendMessage(argumentInvalid(args[0], "status")).queue()
            else
            {
                event.jda.shardManager!!.setStatus(status)
                event.channel.sendMessage("Setting status to `${status.name.toLowerCase()}`. This may take a minute to show up.").queue()
            }
        }
        else
            event.channel.sendMessage("The available statuses are `${OnlineStatus.values().joinToString("`, `") { it.name.toLowerCase() }}`").queue()
    }
}