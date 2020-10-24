package me.qbosst.bossbot.bot.commands.general

import me.qbosst.bossbot.bot.commands.meta.Command
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object InviteCommand : Command(
        "invite",
        "Returns the invite link used to invite boss bot"
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage(event.jda.getInviteUrl(Permission.ADMINISTRATOR)).queue()
    }
}