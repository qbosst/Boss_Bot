package me.qbosst.bossbot.bot.commands.general

import me.qbosst.bossbot.bot.commands.Command
import me.qbosst.bossbot.util.loadClasses
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object InviteCommand : Command(
        "invite"
)
{
    private val permissions: Collection<Permission> = kotlin.run {
        val perms = mutableListOf<Permission>()
        loadClasses("me.qbosst.bossbot.bot.commands", Command::class.java)
                .map{ it.fullBotPermissions }
                .forEach{ perms.addAll(it) }

        perms.distinct()
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage(event.jda.getInviteUrl(permissions)).queue()
    }
}