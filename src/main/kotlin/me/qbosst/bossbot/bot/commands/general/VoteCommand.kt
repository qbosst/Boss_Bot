package me.qbosst.bossbot.bot.commands.general

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.util.getGuildOrNull
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object VoteCommand: Command(
        "vote",
        description = "Provides links to websites you can upvote Boss Bot on",
        guildOnly = false
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        event.channel.sendMessage(
                EmbedBuilder()
                        .setColor(event.getGuildOrNull()?.selfMember?.color)
                        .setAuthor("Enjoying Boss Bot? Upvote it here!", null, event.jda.selfUser.effectiveAvatarUrl)
                        .apply {
                            BotConfig.vote_links.forEach { link ->
                                appendDescription("${link}\n")
                            }
                        }
                        .setFooter("Thank you!")
                        .build()
        ).queue()
    }
}