package qbosst.bossbot.bot.commands.general

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.util.loadClasses

object InviteCommand : Command(
        "invite"
)
{
    private val permissions: Collection<Permission> = kotlin.run {
        val perms = mutableListOf<Permission>()
        loadClasses("qbosst.bossbot.bot.commands", Command::class.java)
                .map{ it.fullBotPermissions }
                .forEach{ perms.addAll(it) }

        perms.distinct()
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        event.channel.sendMessage(event.jda.getInviteUrl(permissions)).queue()
    }
}