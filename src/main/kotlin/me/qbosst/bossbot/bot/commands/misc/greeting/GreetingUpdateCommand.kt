package me.qbosst.bossbot.bot.commands.misc.greeting

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.userNotFound
import me.qbosst.bossbot.bot.userNotMentioned
import me.qbosst.bossbot.database.managers.UserDataManager
import me.qbosst.bossbot.database.managers.getUserData
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.util.getUserByString
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object GreetingUpdateCommand : Command(
        "update",
        description = "Updates a user's greeting message",
        usages = listOf("@user <message>"),
        guildOnly = false,
        developerOnly = true
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>) {
        if (args.isNotEmpty())
        {
            val user = event.jda.shardManager!!.getUserByString(args[0])
            if (user != null)
            {
                if (args.size > 1)
                {
                    val message = args.drop(1).joinToString(" ")
                    if (message.length < UserDataTable.MAX_GREETING_LENGTH)
                    {
                        UserDataManager.update(user, UserDataTable.greeting, message)
                        event.channel.sendMessage("$user greeting has been updated").queue()
                    }
                    else
                        event.channel.sendMessage("too long").queue()
                }
                else
                    event.channel.sendMessage(user.getUserData().greeting ?: "${user.asTag} does not have a greeting message").queue()
            }
            else
                event.channel.sendMessage(userNotFound(args[0])).queue()
        }
        else
            event.channel.sendMessage(userNotMentioned()).queue()
    }

}