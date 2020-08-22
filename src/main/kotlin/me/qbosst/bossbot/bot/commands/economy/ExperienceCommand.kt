package me.qbosst.bossbot.bot.commands.economy

import me.qbosst.bossbot.bot.commands.Command
import me.qbosst.bossbot.bot.userNotFound
import me.qbosst.bossbot.database.data.GuildUserData
import me.qbosst.bossbot.util.getMemberByString
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ExperienceCommand: Command(
        "experience",
        usage = listOf("", "[@user]"),
        examples = listOf("", "@boss"),
        aliases = listOf("xp")
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val target: Member = if(args.isNotEmpty())
        {
            val name = args.joinToString(" ")
            event.guild.getMemberByString(name) ?: kotlin.run {
                event.channel.sendMessage(userNotFound(name)).queue()
                return
            }
        } else event.member!!

        event.channel.sendMessage("${target.user.asTag} has ${GuildUserData.get(target).experience} experience!").queue()
    }
}