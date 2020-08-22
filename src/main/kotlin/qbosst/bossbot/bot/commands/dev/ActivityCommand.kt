package qbosst.bossbot.bot.commands.dev

import net.dv8tion.jda.api.Permission
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
            val type: Activity.ActivityType
            try
            {
                type = Activity.ActivityType.valueOf(args[0].toUpperCase())
            }
            catch (e: Exception)
            {
                event.channel.sendMessage("There was an error while trying to get the activity type. Please make sure the activity type is valid").queue()
                return
            }

            event.jda.presence.activity = Activity.of(type, if(args.size > 1) args.drop(1).joinToString(" ") else kotlin.run
            {
                event.channel.sendMessage("Activity messages cannot be blank!").queue()
                return
            })
            event.channel.sendMessage("Setting the new activity. This may take a minute to show up").queue()
        }
        else
        {
            event.channel.sendMessage("The available activity types are `${Activity.ActivityType.values().joinToString("`, `") { it.name.toLowerCase() }}`").queue()
        }
    }
}