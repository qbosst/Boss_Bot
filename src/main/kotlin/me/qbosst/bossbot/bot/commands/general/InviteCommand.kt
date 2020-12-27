package me.qbosst.bossbot.bot.commands.general

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.loadObjectOrClass
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object InviteCommand : Command(
        "invite",
        description = "Provides the invite link used to invite boss bot",
        usages = listOf(""),
        guildOnly = false
)
{

    private val allPermissions: List<Permission> =
            loadObjectOrClass("${BossBot::class.java.`package`.name}.commands", Command::class.java)
                    .map { it.fullBotPermissions }.flatten().distinct()

    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        event.channel.sendMessage(event.jda.getInviteUrl(allPermissions)).queue()
    }
}