package me.qbosst.bossbot.bot.commands.economy

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.userNotFound
import me.qbosst.bossbot.database.managers.getMemberData
import me.qbosst.bossbot.util.getMemberByString
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ExperienceCommand: Command(
        "experience",
        usage_raw = listOf("[@user]"),
        examples_raw = listOf("", "@boss"),
        aliases_raw = listOf("xp", "exp")
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        val target: Member = if(args.isNotEmpty())
        {
            val name = args.joinToString(" ")
            event.guild.getMemberByString(name) ?: kotlin.run {
                event.channel.sendMessage(userNotFound(name)).queue()
                return
            }
        } else event.member!!

        event.channel.sendMessage("${target.user.asTag} has ${target.getMemberData().experience} experience!").queue()
    }
}