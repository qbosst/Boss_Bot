package me.qbosst.bossbot.bot.commands.general

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.listeners.MessageListener
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object InviteCommand : Command(
        "invite",
        description = "Provides the invite link used to invite boss bot",
        usage_raw = listOf(""),
        guildOnly = false
)
{

    private val allPermissions: List<Permission>
        get() {
            val permissions = mutableListOf<Permission>()
            for(command in MessageListener.allCommands)
                permissions.addAll(command.botPermissions)
            return permissions
        }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage(event.jda.getInviteUrl(allPermissions)).queue()
    }
}