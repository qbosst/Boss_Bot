package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ActivityCommand : DeveloperCommand(
        "activity",
        "Sets the bot's activity"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val type: Activity.ActivityType? = enumValues<Activity.ActivityType>().firstOrNull { it.name.equals(args[0], true) }
            if(type == null)
                event.channel.sendMessage("`${args[0].maxLength()}` is not a valid activity type!").queue()
            else
            {
                event.jda.presence.activity = Activity.of(type, if(args.size > 1) args.drop(1).joinToString(" ") else kotlin.run
                {
                    event.channel.sendMessage("Activity messages cannot be blank!").queue()
                    return
                })
                event.channel.sendMessage("Setting the new activity. This may take a minute to show up").queue()
            }
        }
        else
            event.channel.sendMessage("The available activity types are `${Activity.ActivityType.values().joinToString("`, `") { it.name.toLowerCase() }}`").queue()
    }
}