package me.qbosst.bossbot.bot.commands.misc.greeting

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.managers.UserDataManager
import me.qbosst.bossbot.database.managers.getUserData
import me.qbosst.bossbot.database.tables.UserDataTable
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object GreetingCommand : Command(
        "greeting",
        description = "Shows or sets your custom greeting message",
        usage_raw = listOf("", "<new message>"),
        guildOnly = false
)
{
    init
    {
        addCommand(GreetingUpdateCommand)
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
        {
            val greeting = run {
                val message = args.joinToString(" ").replace(Regex("@"),"@\u200B")
                if(message.length > UserDataTable.max_greeting_length)
                {
                    event.channel.sendMessage("Greeting messages cannot be longer than ${UserDataTable.max_greeting_length} characters!").queue()
                    return
                }
                message
            }

            UserDataManager.update(event.author, UserDataTable.greeting, greeting)
            event.channel.sendMessage("Your greeting has successfully been updated.").queue()
        }
        else
        {
            val greeting = event.author.getUserData().greeting ?: "You do not have a greeting setup!"

            event.channel.sendMessage(greeting).queue()
        }
    }

}