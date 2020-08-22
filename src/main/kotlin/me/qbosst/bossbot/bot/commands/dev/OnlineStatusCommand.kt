package me.qbosst.bossbot.bot.commands.dev

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object OnlineStatusCommand : DeveloperCommand(
        "onlinestatus",
        "Sets the bot's online status"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val status: OnlineStatus
            try
            {
                status = OnlineStatus.valueOf(args.joinToString(" ").toUpperCase())
            }
            catch (e: Exception)
            {
                event.channel.sendMessage("There was an error while trying to set the online status. Please make sure the online status is valid").queue()
                return
            }

            event.jda.presence.setStatus(status)
            event.channel.sendMessage("Setting status to `${status.name.toLowerCase()}`, this may take a minute to show up.").queue()

        }
        else
        {
            event.channel.sendMessage("The available statuses are `${OnlineStatus.values().joinToString("`, `") { it.name.toLowerCase() }}`").queue()
        }
    }
}