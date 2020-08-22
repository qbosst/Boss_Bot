package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.bot.commands.Command
import me.qbosst.bossbot.util.makeSafe
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener

object UnbanCommand : Command(
        "unban",
        userPermissions = listOf(Permission.BAN_MEMBERS),
        botPermissions = listOf(Permission.BAN_MEMBERS)

), EventListener {
    override fun execute(event: MessageReceivedEvent, args: List<String>) {
        val query = if(args.isNotEmpty())
        {
            args.joinToString(" ")
        }
        else
        {
            event.channel.sendMessage("Please provide the id or the tag of the user you would like to unban").queue()
            return
        }

        if(!query.matches(Regex("(<@!?)?\\d{17,19}>?\$")) && !query.matches(Regex(".{2,32}#\\d{4}$")))
        {
            event.channel.sendMessage("That is not a valid id or tag!").queue()
        }
        else
        {
            event.guild.retrieveBanList().map { list ->
                list.map { ban -> ban.user }.firstOrNull { user ->
                    user.idLong == query.replace("\\D+".toRegex(), "").toLong() || user.asTag == query
                }
            }.queue()
            { user ->
                if(user == null)
                {
                    event.channel.sendMessage("I could not find any user banned with the name or tag `${query.makeSafe()}`").queue()
                }
                else
                {
                    event.guild.unban(user).queue()
                    {
                        event.channel.sendMessage("${user.asTag} has been unbanned!").queue()
                    }
                }
            }
        }
    }

    override fun onEvent(event: GenericEvent) {
        when(event)
        {
            is GuildUnbanEvent ->
            {
                //TODO
            }
        }
    }
}