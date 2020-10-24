package me.qbosst.bossbot.bot.commands.general

import me.qbosst.bossbot.bot.commands.meta.Command
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object InviteCommand : Command(
        "invite",
        description = "Provides the invite link used to invite boss bot",
        usage_raw = listOf("")
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage(event.jda.getInviteUrl(Permission.ADMINISTRATOR)).queue()
    }
}