package qbosst.bossbot.bot.commands.economy

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.bot.userNotFound
import qbosst.bossbot.database.data.GuildUserData
import qbosst.bossbot.util.getMemberByString
import qbosst.bossbot.util.makeSafe

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