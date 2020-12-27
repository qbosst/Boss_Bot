package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.argumentMissing
import me.qbosst.bossbot.bot.commands.meta.Command
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ActivityCommand : Command(
        "activity",
        description = "Sets the self-user's activity",
        usages = listOf("<activity> <description> | [url]"),
        examples = listOf(
                "${Activity.ActivityType.WATCHING.name.toLowerCase()} a sports event",
                "${Activity.ActivityType.LISTENING.name.toLowerCase()} to music",
                "${Activity.ActivityType.DEFAULT.name.toLowerCase()} fortnite",
                "${Activity.ActivityType.STREAMING.name.toLowerCase()} minecraft | https://www.twitch.tv/ninja"
        ),
        guildOnly = false,
        developerOnly = true
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
        {
            val type = enumValues<Activity.ActivityType>().firstOrNull { it.name.equals(args[0], true) }
            if(type == null)
                event.channel.sendMessage(argumentInvalid(args[0], "activity")).queue()
            else
            {
                val arguments = args.drop(1).joinToString(" ").split(Regex("[|]"), 2).map { it.trim() }
                val description = arguments.getOrNull(0)
                val url = arguments.getOrNull(1)
                if(description == null)
                    event.channel.sendMessage(argumentMissing("description")).queue()
                else
                {
                    val activity = Activity.of(type, description, url)
                    event.jda.shardManager!!.setActivity(activity)
                    event.channel.sendMessage("Setting the new activity to `${type.name.toLowerCase()}`... This may take a few minutes").queue()
                }
            }
        }
        else
            event.channel.sendMessage("The available activity types are `${Activity.ActivityType.values().joinToString("`, `") { it.name.toLowerCase() }}`").queue()
    }
}