package me.qbosst.bossbot.bot.commands.misc.greeting

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.entities.database.UserData
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object GreetingCommand : Command(
        "greeting",
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_WRITE)
) {
    init {
        addCommand(GreetingUpdateCommand)
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
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

            UserData.update(event.author, UserDataTable.greeting, greeting)
            event.channel.sendMessage("Your greeting has successfully been updated.").queue()
        }
        else
        {
            val greeting = UserData.get(event.author).greeting ?: kotlin.run {
                val default = Config.Values.DEFAULT_GREETING.getString()
                if(default.isNullOrEmpty()) "You do not have a greeting setup!" else default
            }

            event.channel.sendMessage(greeting).queue()
        }
    }

}